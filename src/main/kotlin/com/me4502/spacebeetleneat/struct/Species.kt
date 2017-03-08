package com.me4502.spacebeetleneat.struct

class Species {
    var topFitness = 0
    var staleness = 0
    var averageFitness = 0
    val genomes: MutableList<Genome> = ArrayList()
}