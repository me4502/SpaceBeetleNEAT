package com.me4502.spacebeetleneat

import com.badlogic.gdx.Input
import java.awt.Graphics
import javax.swing.JFrame
import javax.swing.JPanel

val INPUT_WIDTH = 30
val INPUT_HEIGHT = 40

val OUTPUT_INDEX = arrayOf(Input.Keys.A, Input.Keys.S, Input.Keys.W, Input.Keys.D)
val INPUTS = INPUT_WIDTH * INPUT_HEIGHT

val PERTURB_CHANCE = 0.90
val CROSSOVER_CHANCE = 0.75

val POPULATION = 50
val MAX_NODES = 10000

val TIMEOUT_LIMIT = 200

val STALE_SPECIES = 15

var gameRunner : GameRunner? = null

fun main(args: Array<String>) {
    val frame = JFrame()
    frame.isVisible = true

    //frame.add(CustomJPanel())

    gameRunner = GameRunner()
    gameRunner!!.run()
}

fun sigmoid(x: Double) : Double = (1 / (1 + Math.exp(-x)))

class CustomJPanel : JPanel() {

    override fun paint(g: Graphics?) {
        super.paint(g)

        for (i in gameRunner!!.getInputs()) {
            val x = i % INPUT_WIDTH
            val y = Math.floor(i / INPUT_HEIGHT.toDouble()).toInt()
            g?.fillRect(x, y, INPUT_WIDTH, INPUT_HEIGHT)
        }
    }
}