package com.me4502.spacebeetleneat.struct

class Neuron {
    val incoming : MutableList<Gene> = ArrayList()
    var value = 0.0

    override fun toString(): String {
        return "Neuron[incoming=$incoming,value=$value]"
    }
}