package com.me4502.spacebeetleneat

val OUTPUT_NAMES = arrayOf("Up", "Down", "Left", "Right")
val INPUTS = 8

val PERTURB_CHANCE = 0.90
val CROSSOVER_CHANCE = 0.75

val POPULATION = 300
val MAX_NODES = 10000

val TIMEOUT_LIMIT = 20

var gameRunner : GameRunner? = null

fun main(args: Array<String>) {
    gameRunner = GameRunner()
    gameRunner!!.run()
}

fun sigmoid(x: Double) : Double = (1 / (1 + Math.exp(-x)))