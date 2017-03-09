package com.me4502.spacebeetleneat

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.me4502.spacebeetle.Entities.Debris
import com.me4502.spacebeetle.Entities.Player
import com.me4502.spacebeetle.Overlays.StartOverlay
import com.me4502.spacebeetle.SpaceBeetle
import com.me4502.spacebeetle.SpaceBeetleLauncher
import com.me4502.spacebeetle.desktop.DesktopLauncher
import com.me4502.spacebeetleneat.struct.Genome
import com.me4502.spacebeetleneat.struct.Pool
import com.me4502.spacebeetleneat.struct.Species

class GameRunner {

    val GAME_WIDTH = 480
    val GAME_HEIGHT = 640

    var gameInstance : AutomatedSpaceBeetle? = null
    var pool : Pool? = null
    var timeout : Int = 0

    fun getScore(): Int {
        return gameInstance!!.game.score
    }

    fun getInputs(): MutableList<Int> {
        val inputs : MutableList<Int> = ArrayList()

        // 30 * 40
        // -1 = player, 0 = blank, 1 = ground.
        val xWidth = GAME_WIDTH / INPUT_WIDTH
        val yHeight = GAME_HEIGHT / INPUT_HEIGHT

        for (x in 0..GAME_WIDTH-xWidth step xWidth) {
            for (y in 0..GAME_HEIGHT-yHeight step yHeight) {
                var inputValue = 0

                for (entity in gameInstance!!.game.entities) {
                    if (entity is Debris || entity is Player) {
                        val converted = gameInstance!!.camera.project(Vector3(entity.position.x, entity.position.y, 0F))
                        val posX = converted.x
                        val posY = converted.y

                        val rect1 = Rectangle(posX, posY, entity.sprite.width, entity.sprite.height)
                        val rect2 = Rectangle(x.toFloat(), y.toFloat(), xWidth.toFloat(), yHeight.toFloat())

                        if (rect1.overlaps(rect2)) {
                            inputValue = if (entity is Debris) 1 else -1
                            break
                        }
                    }
                }

                inputs.add(inputValue)
            }
        }

        return inputs
    }

    fun run() {
        if (pool == null) {
            pool = Pool()

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
        pool!!.currentFrame = 0
        timeout = TIMEOUT_LIMIT

        val species = pool!!.species[pool!!.currentSpecies]
        val genome = species.genomes[pool!!.currentGenome]
        genome.generateNetwork()
        if (gameInstance != null) {
            evaluateCurrent()
        }
    }

    fun evaluateCurrent() {
        val species = pool!!.species[pool!!.currentSpecies]
        val genome = species.genomes[pool!!.currentGenome]

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
        for (species in pool!!.species) {
            if (!foundSpecies && child.isSameSpecies(species.genomes[0])) {
                species.genomes.add(child)
                foundSpecies = true
                break
            }
        }

        if (!foundSpecies) {
            val childSpecies = Species()
            childSpecies.genomes.add(child)
            pool!!.species.add(childSpecies)
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

            val pool = gameRunner!!.pool
            val species = pool!!.species[pool.currentSpecies]
            val genome = species.genomes[pool.currentGenome]

            if (oldScore < gameRunner!!.getScore()) {
                oldScore = gameRunner!!.getScore()
                gameRunner!!.timeout = TIMEOUT_LIMIT
            }

            gameRunner!!.timeout -= 1

            if (pool.currentFrame % 20 == 0) {
                gameRunner!!.evaluateCurrent()
            }

            if (gameRunner!!.timeout <= 0 || (overlay != null && overlay.timer == 0)) {
                // This run is over.
                if (overlay == null) {
                    // If timeout, reset the game.
                    overlay = StartOverlay(true)
                }

                // Get rid of the game start overlay.
                overlay.interact(-90, -90)

                // The fitness heavily favours score. Time is in there only so that it favours the faster of two identical scores.
                val fitness = (SpaceBeetle.inst().getFileStorage().lastscore * 2) - (pool.currentFrame / 60)
                genome.fitness = fitness
                if (fitness > pool.maxFitness) {
                    pool.maxFitness = fitness
                }

                pool.currentSpecies = 0
                pool.currentGenome = 0
                while (pool.hasFitnessBeenMeasured()) {
                    pool.nextGenome()
                }

                println("Species: ${pool.currentSpecies}, Genome: ${pool.currentGenome}, Fitness: $fitness, Frame: ${pool
                        .currentFrame}")

                gameRunner!!.initializeRun()
            }

            pool.currentFrame += 1
        }
    }
}