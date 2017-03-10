package com.me4502.spacebeetleneat

import com.badlogic.gdx.Input
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JToggleButton

val GAME_WIDTH = 480
val GAME_HEIGHT = 640

val INPUT_WIDTH = 60
val INPUT_HEIGHT = 80

val OUTPUT_INDEX = arrayOf(Input.Keys.A, Input.Keys.S, Input.Keys.W, Input.Keys.D)
val INPUTS = (INPUT_WIDTH * INPUT_HEIGHT) + 2

val PERTURB_CHANCE = 0.90
val CROSSOVER_CHANCE = 0.75

val POPULATION = 200
val MAX_NODES = 1000000

val TIMEOUT_LIMIT = 50

val STALE_SPECIES = 15

val FRAMES_PER_UPDATE = 1

var gameRunner : GameRunner? = null

fun main(args: Array<String>) {
    val frame = JFrame()
    frame.isVisible = true
    frame.size = Dimension(GAME_WIDTH * 2, GAME_HEIGHT * 2)
    frame.layout = FlowLayout()

    val panel = CustomJPanel()
    panel.preferredSize = Dimension(GAME_WIDTH, GAME_HEIGHT)
    frame.add(panel)

    val showButton = JToggleButton("Show Inputs")
    frame.add(showButton)

    val fitnessLabel = JLabel("Fitness: ")
    frame.add(fitnessLabel)

    val maxFitnessLabel = JLabel("Max Fitness: ")
    frame.add(maxFitnessLabel)

    gameRunner = GameRunner({
        if (showButton.isSelected) {
            panel.repaint()
            panel.isVisible = true
        } else {
            panel.isVisible = false
        }

        val pool = gameRunner!!.population
        val species = pool!!.species[pool.currentSpecies]
        val genome = species.genomes[pool.currentGenome]

        fitnessLabel.text = "Fitness: ${genome.fitness}"
        maxFitnessLabel.text = "Max Fitness: ${pool.maxFitness}"
    })
    gameRunner!!.run()
}

fun sigmoid(x: Double) : Double = (1 / (1 + Math.exp(-x)))

class CustomJPanel : JPanel(true) {

    override fun paint(g: Graphics?) {
        super.paint(g)

        for ((i, value) in gameRunner!!.lastInput.withIndex()) {
            val x = i % INPUT_WIDTH
            val y = Math.floor(i / INPUT_HEIGHT.toDouble()).toInt()
            if (value == -1) {
                g?.color = Color.GREEN
            } else if (value == 0) {
                g?.color = Color.WHITE
            } else if (value == 1) {
                g?.color = Color.BLACK
            } else {
                g?.color = Color.BLUE
            }
            g?.fillRect(x * (GAME_WIDTH / INPUT_WIDTH), y * (GAME_HEIGHT / INPUT_HEIGHT), (GAME_WIDTH / INPUT_WIDTH), (GAME_HEIGHT / INPUT_HEIGHT))
        }
    }
}