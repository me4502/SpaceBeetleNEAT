package com.me4502.spacebeetleneat

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.me4502.spacebeetle.SpaceBeetle
import com.me4502.spacebeetle.SpaceBeetleLauncher
import com.me4502.spacebeetle.desktop.DesktopLauncher
import com.me4502.spacebeetleneat.struct.Genome
import com.me4502.spacebeetleneat.struct.Pool
import com.me4502.spacebeetleneat.struct.Species

class GameRunner {

    var gameInstance : AutomatedSpaceBeetle? = null
    var pool : Pool? = null
    var timeout : Int = 0

    fun getScore(): Int {
        return gameInstance!!.game.score
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

        val config = Lwjgl3ApplicationConfiguration()
        config.setTitle("SpaceBeetle Runner")
        config.setWindowedMode(120 * 4, 160 * 4)
        gameInstance = AutomatedSpaceBeetle(DesktopLauncher())
        Lwjgl3Application(gameInstance, config)
    }

    fun initializeRun() {
        // TODO load
        pool!!.currentFrame = 0
        timeout = TIMEOUT_LIMIT

        val species = pool!!.species[pool!!.currentSpecies]
        val genome = species.genomes[pool!!.currentGenome]
        genome.generateNetwork()
        evaluateCurrent()
    }

    fun evaluateCurrent() {
        val species = pool!!.species[pool!!.currentSpecies]
        val genome = species.genomes[pool!!.currentGenome]

        //var inputs = getInputs()
        //var outputs = genome.network!!.evaluate(inputs)
        //TODO validate outputs - cancel out left/right etc. Then pass to program
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

        override fun render() {
            super.render()


        }
    }
}