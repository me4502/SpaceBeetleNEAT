package com.me4502.spacebeetleneat.struct

import com.me4502.spacebeetleneat.*
import java.util.concurrent.ThreadLocalRandom

val MUTATE_CONNECTIONS_CHANCE = 0.25
val LINK_MUTATION_CHANCE = 2.0
val BIAS_MUTATION_CHANCE = 0.40
val ENABLE_MUTATION_CHANCE = 0.2
val DISABLE_MUTATION_CHANCE = 0.4
val NODE_MUTATION_CHANCE = 0.50
val STEP_SIZE = 0.1

val DELTA_DISJOINT = 2.0
val DELTA_WEIGHTS = 0.4
val DELTA_THRESHOLD = 1.0

class Genome : Cloneable {
    val genes : MutableList<Gene> = ArrayList()
    var network : Network? = null
    var fitness = Int.MIN_VALUE
    var maxNeuron = 0
    var globalRank = 0
    var mutateConnectionsChance = MUTATE_CONNECTIONS_CHANCE
    var linkMutationChance = LINK_MUTATION_CHANCE
    var biasMutationChance = BIAS_MUTATION_CHANCE
    var enableMutationChance = ENABLE_MUTATION_CHANCE
    var disableMutationChance = DISABLE_MUTATION_CHANCE
    var nodeMutationChance = NODE_MUTATION_CHANCE
    var stepSize = STEP_SIZE

    fun generateNetwork() {
        val net = Network()

        for (i in 0..INPUTS) {
            net.neurons[i] = Neuron()
        }

        for (i in 0..OUTPUT_INDEX.size) {
            net.neurons[MAX_NODES + i] = Neuron()
        }

        this.genes.sortBy(Gene::output)

        for (gene in this.genes) {
            if (gene.enabled) {
                if (!net.neurons.containsKey(gene.output)) {
                    net.neurons[gene.output] = Neuron()
                }
                val neuron = net.neurons[gene.output]!!
                neuron.incoming.add(gene)
                if (!net.neurons.containsKey(gene.input)) {
                    net.neurons[gene.input] = Neuron()
                }
            }
        }

        this.network = net
    }

    fun isSameSpecies(otherGenome : Genome) : Boolean {
        val deltaDisjoint = DELTA_DISJOINT * disjointGenes(otherGenome.genes)
        val deltaWeights = DELTA_WEIGHTS * weightGenes(otherGenome.genes)
        return deltaDisjoint + deltaWeights < DELTA_THRESHOLD
    }

    fun disjointGenes(otherGenes : List<Gene>) : Int {
        val innovations : MutableMap<Int, Boolean> = HashMap()

        for (gene in genes) {
            innovations.put(gene.innovation, true)
        }

        val otherInnovations : MutableMap<Int, Boolean> = HashMap()
        for (otherGene in otherGenes) {
            otherInnovations.put(otherGene.innovation, true)
        }

        var disjointGenes = 0

        genes
                .filterNot { otherInnovations.getOrDefault(it.innovation, true) }
                .forEach { disjointGenes += 1 }

        otherGenes
                .filterNot { innovations.getOrDefault(it.innovation, true) }
                .forEach { disjointGenes += 1 }

        return disjointGenes / (Math.max(genes.size, otherGenes.size))
    }

    /**
     * Calculate the average difference in weights between the current and given set of genes.
     */
    fun weightGenes(otherGenes : List<Gene>) : Double {
        val otherInnovations : MutableMap<Int, Gene> = HashMap()

        for (otherGene in otherGenes) {
            otherInnovations.put(otherGene.innovation, otherGene)
        }

        var weightDelta = 0.0
        var occurences = 0

        for (gene in genes) {
            if (otherInnovations.containsKey(gene.innovation)) {
                val otherGene = otherInnovations[gene.innovation]
                if (otherGene != null) {
                    weightDelta += Math.abs(gene.weight - otherGene.weight)
                    occurences += 1
                }
            }
        }

        return weightDelta / occurences
    }

    fun setCanMutate(enable : Boolean) {
        val candidates : MutableList<Gene> = ArrayList()
        genes.filterTo(candidates) { it.enabled != enable }

        if (candidates.isEmpty()) {
            return
        }

        val gene = candidates[ThreadLocalRandom.current().nextInt(candidates.size)]
        gene.enabled = !gene.enabled
    }

    fun pointMutate() {
        val step = stepSize

        for (gene in genes) {
            if (ThreadLocalRandom.current().nextDouble() < PERTURB_CHANCE) {
                gene.weight = gene.weight + ThreadLocalRandom.current().nextDouble() * step * 2 - step
            } else {
                gene.weight = ThreadLocalRandom.current().nextDouble() * 4 - 2
            }
        }
    }

    fun randomNeuron(nonInput : Boolean) : Int {
        val neurons : MutableMap<Int, Boolean> = HashMap()
        if (!nonInput) {
            for (i in 0..INPUTS) {
                neurons[i] = true
            }
        }

        for (o in 0..OUTPUT_INDEX.size) {
            neurons[MAX_NODES + o] = true
        }

        for (gene in genes) {
            if (!nonInput || gene.input > INPUTS) {
                neurons[gene.input] = true
            } else if (!nonInput || gene.output > INPUTS) {
                neurons[gene.output] = true
            }
        }

        val n = ThreadLocalRandom.current().nextInt(neurons.size)
        return neurons.keys.elementAt(n)
    }

    fun containsLink(newLink : Gene) : Boolean {
        return genes.any { it.input == newLink.input && it.output == newLink.output }
    }

    fun linkMutate(forceBias : Boolean) {
        var neuron1 = randomNeuron(false)
        var neuron2 = randomNeuron(true)

        val newLink = Gene()
        if (neuron1 <= INPUTS && neuron2 <= INPUTS) {
            return
        }

        if (neuron2 <= INPUTS) {
            val temp = neuron1
            neuron1 = neuron2
            neuron2 = temp
        }

        newLink.input = neuron1
        newLink.output = neuron2
        if (forceBias) {
            newLink.input = INPUTS
        }

        if (containsLink(newLink)) {
            return
        }

        newLink.innovation = gameRunner!!.population!!.innovate()
        newLink.weight = ThreadLocalRandom.current().nextDouble() * 4.0 - 2.0

        genes.add(newLink)
    }

    fun nodeMutate() {
        if (genes.isEmpty()) {
            return
        }

        maxNeuron += 1

        val gene = genes[ThreadLocalRandom.current().nextInt(genes.size)]
        if (!gene.enabled) {
            return
        }
        gene.enabled = false

        val gene1 = gene.clone()
        gene1.output = maxNeuron
        gene1.weight = 1.0
        gene1.innovation = gameRunner!!.population!!.innovate()
        gene1.enabled = true
        genes.add(gene1)

        val gene2 = gene.clone()
        gene2.input = maxNeuron
        gene2.innovation = gameRunner!!.population!!.innovate()
        gene2.enabled = true
        genes.add(gene2)
    }

    fun mutate() {
        if (ThreadLocalRandom.current().nextBoolean()) {
            this.mutateConnectionsChance *= 0.95
        } else {
            this.mutateConnectionsChance *= 1.05263
        }

        if (ThreadLocalRandom.current().nextBoolean()) {
            this.linkMutationChance *= 0.95
        } else {
            this.linkMutationChance *= 1.05263
        }

        if (ThreadLocalRandom.current().nextBoolean()) {
            this.biasMutationChance *= 0.95
        } else {
            this.biasMutationChance *= 1.05263
        }

        if (ThreadLocalRandom.current().nextBoolean()) {
            this.enableMutationChance *= 0.95
        } else {
            this.enableMutationChance *= 1.05263
        }

        if (ThreadLocalRandom.current().nextBoolean()) {
            this.disableMutationChance *= 0.95
        } else {
            this.disableMutationChance *= 1.05263
        }

        if (ThreadLocalRandom.current().nextBoolean()) {
            this.nodeMutationChance *= 0.95
        } else {
            this.nodeMutationChance *= 1.05263
        }

        if (ThreadLocalRandom.current().nextDouble() < mutateConnectionsChance) {
            pointMutate()
        }

        var p = linkMutationChance
        while (p > 0) {
            if (ThreadLocalRandom.current().nextDouble() < p) {
                linkMutate(false)
            }
            p -= 1
        }

        p = biasMutationChance
        while (p > 0) {
            if (ThreadLocalRandom.current().nextDouble() < p) {
                linkMutate(true)
            }
            p -= 1
        }

        p = nodeMutationChance
        while (p > 0) {
            if (ThreadLocalRandom.current().nextDouble() < p) {
                nodeMutate()
            }
            p -= 1
        }

        p = enableMutationChance
        while (p > 0) {
            if (ThreadLocalRandom.current().nextDouble() < p) {
                setCanMutate(true)
            }
            p -= 1
        }

        p = disableMutationChance
        while (p > 0) {
            if (ThreadLocalRandom.current().nextDouble() < p) {
                setCanMutate(false)
            }
            p -= 1
        }
    }

    public override fun clone(): Genome {
        val genome = Genome()
        for (gene in this.genes) {
            genome.genes.add(gene.clone())
        }

        genome.maxNeuron = this.maxNeuron
        genome.mutateConnectionsChance = this.mutateConnectionsChance
        genome.linkMutationChance = this.linkMutationChance
        genome.biasMutationChance = this.biasMutationChance
        genome.nodeMutationChance = this.nodeMutationChance
        genome.enableMutationChance = this.enableMutationChance
        genome.disableMutationChance = this.disableMutationChance

        return genome
    }

    override fun toString(): String {
        return "Genome[fitness=$fitness,genes=$genes,globalRank=$globalRank]"
    }
}