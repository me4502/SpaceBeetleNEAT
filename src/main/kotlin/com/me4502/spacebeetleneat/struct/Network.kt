package com.me4502.spacebeetleneat.struct

import com.me4502.spacebeetleneat.INPUTS
import com.me4502.spacebeetleneat.MAX_NODES
import com.me4502.spacebeetleneat.OUTPUT_INDEX
import com.me4502.spacebeetleneat.sigmoid

class Network {
    val neurons : MutableMap<Int, Neuron> = HashMap()

    fun evaluate(inputs : MutableList<Int>) : MutableMap<Int, Boolean> {
        if (inputs.size != INPUTS) {
            println("Wrong input format. Got ${inputs.size} expected $INPUTS.")
            return HashMap()
        }

        for (i in 0..INPUTS-1) {
            this.neurons[i]?.value = inputs[i].toDouble()
        }

        for (neuron in this.neurons.values) {
            var sum = 0.0
            for (i in 0..neuron.incoming.size-1) {
                val incoming = neuron.incoming[i]
                val other = this.neurons[incoming.input]
                sum += incoming.weight * other!!.value
            }

            if (neuron.incoming.size > 0) {
                neuron.value = sigmoid(sum)
            }
        }

        val outputs : MutableMap<Int, Boolean> = HashMap()

        for (i in 0..OUTPUT_INDEX.size-1) {
            val button = OUTPUT_INDEX[i]
            outputs[button] = this.neurons[MAX_NODES + i]!!.value > 0
        }

        return outputs
    }
}
