package com.me4502.spacebeetleneat

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.me4502.spacebeetle.Entities.Debris
import com.me4502.spacebeetle.Overlays.StartOverlay
import com.me4502.spacebeetle.SpaceBeetle
import com.me4502.spacebeetle.SpaceBeetleLauncher
import com.me4502.spacebeetle.desktop.DesktopLauncher
import com.me4502.spacebeetleneat.struct.Genome
import com.me4502.spacebeetleneat.struct.Population
import com.me4502.spacebeetleneat.struct.Species
import java.io.File
import java.io.PrintWriter

class GameRunner(var callback: () -> Unit) {
    var gameInstance : AutomatedSpaceBeetle? = null
    var population: Population? = null
    var timeout : Int = 0

    var lastInput : MutableList<Int> = ArrayList()

    val runFolder = System.currentTimeMillis()

    fun getScore(): Int {
        return gameInstance!!.game.score
    }

    fun getInputs(): MutableList<Int> {
        val inputs : MutableList<Int> = ArrayList()

        // 30 * 40
        // -1 = player, 0 = blank, 1 = ground.
        val xWidth = GAME_WIDTH / INPUT_WIDTH
        val yHeight = GAME_HEIGHT / INPUT_HEIGHT

        for (y in 0..GAME_HEIGHT-yHeight step yHeight) {
            for (x in 0..GAME_WIDTH-xWidth step xWidth) {
                var inputValue = 0

                var converted = gameInstance!!.camera.project(Vector3(gameInstance!!.game.player.sprite.x, gameInstance!!.game.player.sprite.y, 0F))
                var posX = converted.x
                var posY = converted.y

                var rect1 = Rectangle(posX, posY, gameInstance!!.game.player.sprite.width, gameInstance!!.game.player.sprite.height)
                val rect2 = Rectangle(x.toFloat(), y.toFloat(), xWidth.toFloat(), yHeight.toFloat())

                if (rect1.overlaps(rect2)) {
                    inputValue = -1
                } else {
                    for (entity in gameInstance!!.game.entities) {
                        if (entity is Debris) {
                            converted = gameInstance!!.camera.project(Vector3(entity.sprite.x, entity.sprite.y, 0F))
                            posX = converted.x
                            posY = converted.y

                            rect1 = Rectangle(posX, posY, entity.sprite.width, entity.sprite.height)

                            if (rect1.overlaps(rect2)) {
                                inputValue = 1
                                break
                            }
                        }
                    }
                }

                inputs.add(inputValue)
            }
        }

        inputs.add(gameInstance!!.game.player.position.x.toInt())
        inputs.add(gameInstance!!.game.player.position.y.toInt())

        lastInput.clear()
        lastInput.addAll(inputs)

        return inputs
    }

    fun run() {
        if (population == null) {
            population = Population()

            for (i in 0..POPULATION) {
                val basic = createBasicGenome()
                addToSpecies(basic)
            }

            initializeRun()
        }

        val config = LwjglApplicationConfiguration()
        config.title = "SpaceBeetle Runner"
        config.width = GAME_WIDTH
        config.height = GAME_HEIGHT
        gameInstance = AutomatedSpaceBeetle(DesktopLauncher())
        LwjglApplication(gameInstance, config)
    }

    fun initializeRun() {
        // TODO load
        population!!.currentFrame = 0
        timeout = TIMEOUT_LIMIT

        val species = population!!.species[population!!.currentSpecies]
        val genome = species.genomes[population!!.currentGenome]
        genome.generateNetwork()
        if (gameInstance != null) {
            evaluateCurrent()
        }
    }

    fun evaluateCurrent() {
        val species = population!!.species[population!!.currentSpecies]
        val genome = species.genomes[population!!.currentGenome]

        val inputs = getInputs()
        val outputs = genome.network!!.evaluate(inputs)

        if (outputs[Input.Keys.W] == true && outputs[Input.Keys.S] == true) {
            outputs[Input.Keys.W] = false
            outputs[Input.Keys.S] = false
        }

        if (outputs[Input.Keys.A] == true && outputs[Input.Keys.D] == true) {
            outputs[Input.Keys.A] = false
            outputs[Input.Keys.D] = false
        }

        for ((key, isDown) in outputs) {
            if (isDown) {
                Gdx.input.inputProcessor.keyDown(key)
            }
        }

    }

    fun addToSpecies(child : Genome) {
        var foundSpecies = false
        for (species in population!!.species) {
            if (!foundSpecies && child.isSameSpecies(species.genomes[0])) {
                species.genomes.add(child)
                foundSpecies = true
                break
            }
        }

        if (!foundSpecies) {
            val childSpecies = Species()
            childSpecies.genomes.add(child)
            population!!.species.add(childSpecies)
        }
    }

    fun createBasicGenome() : Genome {
        val genome = Genome()

        genome.maxNeuron = INPUTS
        genome.mutate()

        return genome
    }

    class AutomatedSpaceBeetle(launcher: SpaceBeetleLauncher) : SpaceBeetle(launcher) {

        var oldScore : Int = 0

        override fun render() {
            super.render()

            val pool = gameRunner!!.population
            val species = pool!!.species[pool.currentSpecies]
            val genome = species.genomes[pool.currentGenome]

            if (oldScore < gameRunner!!.getScore()) {
                oldScore = gameRunner!!.getScore()
                gameRunner!!.timeout = TIMEOUT_LIMIT
            }

            if (pool.currentFrame % FRAMES_PER_UPDATE == 0) {
                gameRunner!!.timeout -= 1

                gameRunner!!.evaluateCurrent()

                if (gameRunner!!.timeout <= 0 || (overlay != null && overlay.timer == 0)) {
                    // This run is over.
                    if (overlay == null) {
                        // If timeout, reset the game.
                        overlay = StartOverlay(true)
                    }

                    // Get rid of the game start overlay.
                    for (i in 0..200) {
                        overlay.interact(-90, -90)
                    }

                    // The fitness heavily favours score. Time is in there only so that it favours the faster of two identical scores.
                    val fitness = (SpaceBeetle.inst().getFileStorage().lastscore * 2) - (pool.currentFrame / 60)
                    genome.fitness = fitness
                    if (fitness > pool.maxFitness) {
                        pool.maxFitness = fitness
                        gameRunner!!.save("backup.${pool.generation}.dat")
                    }

                    println("Species: ${pool.currentSpecies}, Genome: ${pool.currentGenome}, Fitness: $fitness, Frame: ${pool
                            .currentFrame}")

                    gameRunner!!.callback.invoke()

                    pool.currentSpecies = 0
                    pool.currentGenome = 0
                    while (pool.hasFitnessBeenMeasured()) {
                        pool.nextGenome()
                    }

                    gameRunner!!.initializeRun()
                }
            }

            pool.currentFrame += 1
        }
    }

    fun save(filename : String) {
        val file : File = File(File("state", runFolder.toString()), filename)
        file.parentFile.mkdirs()
        val writer : PrintWriter = PrintWriter(file)

        writer.println(population!!.generation)
        writer.println(population!!.maxFitness)
        writer.println(population!!.species.size)
        for (species in population!!.species) {
            writer.println(species.topFitness)
            writer.println(species.staleness)
            writer.println(species.genomes.size)
            for (genome in species.genomes) {
                writer.println(genome.fitness)
                writer.println(genome.maxNeuron)
                writer.println(genome.biasMutationChance)
                writer.println(genome.disableMutationChance)
                writer.println(genome.enableMutationChance)
                writer.println(genome.linkMutationChance)
                writer.println(genome.mutateConnectionsChance)
                writer.println(genome.nodeMutationChance)
                writer.println(genome.genes.size)
                for (gene in genome.genes) {
                    writer.println(gene.input)
                    writer.println(gene.output)
                    writer.println(gene.weight)
                    writer.println(gene.innovation)
                    writer.println(gene.enabled)
                }
            }
        }

        writer.close()
    }
}