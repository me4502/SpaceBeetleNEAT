package com.me4502.spacebeetleneat.struct

class Gene : Cloneable {
    var input = 0
    var output = 0
    var weight = 0.0
    var enabled = true
    var innovation = 0

    public override fun clone(): Gene {
        val gene = Gene()
        gene.input = input
        gene.output = output
        gene.weight = weight
        gene.enabled = enabled
        gene.innovation = innovation
        return gene
    }
}