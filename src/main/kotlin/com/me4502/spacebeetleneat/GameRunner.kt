package com.me4502.spacebeetleneat

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.math.Rectangle
import com.me4502.spacebeetle.Entities.Debris
import com.me4502.spacebeetle.Entities.Player
import com.me4502.spacebeetle.Overlays.StartOverlay
import com.me4502.spacebeetle.SpaceBeetle
import com.me4502.spacebeetle.SpaceBeetle.AI_MODE
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
        val xWidth = 16
        val yHeight = 16

        val player : Player = SpaceBeetle.inst().game.player

        for (y in -17..17 step 17) {
            for (x in -16..16 step 16) {
                val rect2 = Rectangle(x.toFloat() + player.position.x, y.toFloat() + player.position.y, xWidth.toFloat(), yHeight.toFloat())

                val inputValue = if (gameInstance!!.game.entities
                        .filterIsInstance<Debris>()
                        .map { Rectangle(it.sprite.x, it.sprite.y, it.sprite.width, it.sprite.height) }
                        .any { it.overlaps(rect2) }) 1 else 0

                inputs.add(inputValue)
            }
        }

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
        config.foregroundFPS = 60
        config.backgroundFPS = Integer.MAX_VALUE
        config.vSyncEnabled = false
        AI_MODE = true
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

                    val fitness = (SpaceBeetle.inst().getFileStorage().lastscore) // Fitness is just score.
                    genome.fitness = fitness
                    if (fitness > pool.maxFitness) {
                        pool.maxFitness = fitness
                        gameRunner!!.save("backup.${pool.generation}.dat")
                    }

                    println("Generation: ${pool.generation} Species: ${pool.currentSpecies}, Genome: ${pool.currentGenome}, Fitness: $fitness, Frame: ${pool
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

        override fun dispose() {
            super.dispose()

            gameRunner!!.save("../last.state.dat")
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