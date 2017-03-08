package com.me4502.spacebeetleneat.struct

import com.me4502.spacebeetleneat.OUTPUT_NAMES

class Pool {
    val species : MutableList<Species> = ArrayList()
    var generation = 0
    var innovation = OUTPUT_NAMES.size
    var currentSpecies = 1
    var currentGenome = 1
    var currentFrame = 0
    var maxFitness = 0

    fun innovate() : Int {
        this.innovation ++
        return innovation
    }
}