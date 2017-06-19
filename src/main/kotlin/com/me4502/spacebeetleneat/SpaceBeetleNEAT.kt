package com.me4502.spacebeetleneat

import com.badlogic.gdx.Input
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JFrame
import javax.swing.JLabel

val GAME_WIDTH = 480
val GAME_HEIGHT = 640

val OUTPUT_INDEX = arrayOf(Input.Keys.A, Input.Keys.S, Input.Keys.W, Input.Keys.D)
val INPUTS = 25

val PERTURB_CHANCE = 0.90
val CROSSOVER_CHANCE = 0.75

val POPULATION = 300
val MAX_NODES = 1000000

val TIMEOUT_LIMIT = 30

val STALE_SPECIES = 15

// How often should we consider moving the player?
val FRAMES_PER_UPDATE = 10

var gameRunner : GameRunner? = null

fun main(args: Array<String>) {
    val frame = JFrame()
    frame.isVisible = true
    frame.size = Dimension(400, 600)
    frame.layout = FlowLayout()

    val fitnessLabel = JLabel("Current Fitness: ")
    frame.add(fitnessLabel)

    val maxFitnessLabel = JLabel("Max Fitness: ")
    frame.add(maxFitnessLabel)

    gameRunner = GameRunner({
        val pool = gameRunner!!.population
        val species = pool!!.species[pool.currentSpecies]
        val genome = species.genomes[pool.currentGenome]

        fitnessLabel.text = "Fitness: ${genome.fitness}"
        maxFitnessLabel.text = "Max Fitness: ${pool.maxFitness}"
    })
    gameRunner!!.run()
}

fun sigmoid(x: Double) : Double = (1.0 / (1.0 + Math.exp(-x)))