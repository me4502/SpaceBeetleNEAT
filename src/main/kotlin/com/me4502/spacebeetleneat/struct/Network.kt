package com.me4502.spacebeetleneat.struct

import com.me4502.spacebeetleneat.INPUTS
import com.me4502.spacebeetleneat.MAX_NODES
import com.me4502.spacebeetleneat.OUTPUT_NAMES
import com.me4502.spacebeetleneat.sigmoid

class Network {
    val neurons : MutableMap<Int, Neuron> = HashMap()

    fun evaluate(inputs : MutableList<Double>) : Map<String, Boolean> {
        inputs.add(1.0)
        if (inputs.size != INPUTS) {
            println("Wrong input format.")
            return HashMap()
        }

        for (i in 0..INPUTS) {
            this.neurons[i]?.value = inputs[i]
        }

        for (neuron in this.neurons.values) {
            var sum = 0.0
            for (i in 0..neuron.incoming.size) {
                val incoming = neuron.incoming[i]
                val other = this.neurons[incoming.input]
                sum += incoming.weight * other!!.value
            }

            if (neuron.incoming.size > 0) {
                neuron.value = sigmoid(sum)
            }
        }

        val outputs : MutableMap<String, Boolean> = HashMap()

        for (i in 0..OUTPUT_NAMES.size) {
            val button = OUTPUT_NAMES[i]
            outputs[button] = this.neurons[MAX_NODES + i]!!.value > 0
        }

        return outputs
    }
}
