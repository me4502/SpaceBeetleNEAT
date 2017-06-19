package com.me4502.spacebeetleneat.struct

import com.me4502.spacebeetleneat.*
import java.util.concurrent.ThreadLocalRandom

class Population {
    val species : MutableList<Species> = ArrayList()
    var generation = 0
    var innovation = OUTPUT_INDEX.size
    var currentSpecies = 0
    var currentGenome = 0
    var currentFrame = 0
    var maxFitness = 0

    fun innovate() : Int {
        this.innovation ++
        return innovation
    }

    fun nextGenome() {
        currentGenome += 1
        if (currentGenome >= species[currentSpecies].genomes.size) {
            currentGenome = 0
            currentSpecies += 1
            if (currentSpecies >= species.size) {
                newGeneration()
                currentSpecies = 0
            }
        }
    }

    fun cullSpecies(cutToOne : Boolean) {
        for (specie in species) {
            specie.genomes.sortByDescending(Genome::fitness)
            var remaining = Math.ceil(specie.genomes.size / 2.0).toInt()
            if (cutToOne) {
                remaining = 1
            }
            while (specie.genomes.size > remaining) {
                specie.genomes.removeAt(specie.genomes.size - 1)
            }
        }
    }

    fun totalAverageFitness() : Int {
        return species.sumBy { it.averageFitness }
    }

    fun calculateAverageFitness(specie: Species) {
        val total = specie.genomes.sumBy { it.globalRank }

        specie.averageFitness = total / specie.genomes.size
    }

    fun removeWeakSpecies() {
        val survived : MutableList<Species> = ArrayList()

        val sum = totalAverageFitness()
        for (specie in species) {
            val breed = Math.floor(specie.averageFitness / sum.toDouble() * POPULATION).toInt()
            if (breed >= 1) {
                survived.add(specie)
            }
        }

        species.clear()
        species.addAll(survived)
    }

    fun rankGlobally() {
        val global : MutableList<Genome> = ArrayList()
        species.flatMapTo(global) { it.genomes }

        global.sortBy(Genome::fitness)

        for ((i, glob) in global.withIndex()) {
            glob.globalRank = i
        }
    }

    fun removeStaleSpecies() {
        val survived : MutableList<Species> = ArrayList()

        for (specie in species) {
            specie.genomes.sortByDescending(Genome::fitness)

            if (specie.genomes[0].fitness > specie.topFitness) {
                specie.topFitness = specie.genomes[0].fitness
                specie.staleness = 0
            } else {
                specie.staleness += 1
            }
            if (specie.staleness < STALE_SPECIES || specie.topFitness >= maxFitness) {
                survived.add(specie)
            }
        }

        species.clear()
        species.addAll(survived)
    }

    fun newGeneration() {
        cullSpecies(false)
        rankGlobally()
        removeStaleSpecies()
        rankGlobally()
        for (specie in species) {
            calculateAverageFitness(specie)
        }
        removeWeakSpecies()
        val sum = totalAverageFitness()
        val children: MutableList<Genome> = ArrayList()

        for (specie in species) {
            val breed = (Math.floor(specie.averageFitness / sum.toDouble() * POPULATION) - 1).toInt()
            for (i in 0..breed-1) {
                children.add(breedChild(specie))
            }
        }

        cullSpecies(true)

        while (children.size + species.size < POPULATION) {
            val specie = species[ThreadLocalRandom.current().nextInt(species.size)]
            children.add(breedChild(specie))
        }

        for (child in children) {
            gameRunner!!.addToSpecies(child)
        }

        generation += 1

        gameRunner!!.save("backup.$generation.dat")
    }

    fun crossover(g1in : Genome, g2in: Genome) : Genome {
        var g1 = g1in
        var g2 = g2in

        if (g2.fitness > g1.fitness) {
            val tempg = g1
            g1 = g2
            g2 = tempg
        }

        val child = Genome()

        val innovations2 : MutableMap<Int, Gene> = HashMap()

        for (gene2 in g2.genes) {
            innovations2[gene2.innovation] = gene2
        }

        for (gene1 in g1.genes) {
            val gene2 = innovations2[gene1.innovation]
            if (gene2 != null && ThreadLocalRandom.current().nextBoolean() && gene2.enabled) {
                child.genes.add(gene2.clone())
            } else {
                child.genes.add(gene1.clone())
            }
        }

        child.maxNeuron = Math.max(g1.maxNeuron, g2.maxNeuron)

        child.biasMutationChance = g1.biasMutationChance
        child.disableMutationChance = g1.disableMutationChance
        child.enableMutationChance = g1.enableMutationChance
        child.linkMutationChance = g1.linkMutationChance
        child.mutateConnectionsChance = g1.mutateConnectionsChance

        return child
    }

    fun breedChild(specie : Species): Genome {
        val child : Genome
        if (ThreadLocalRandom.current().nextDouble() < CROSSOVER_CHANCE) {
            val g1 = specie.genomes[ThreadLocalRandom.current().nextInt(specie.genomes.size)].clone()
            val g2 = specie.genomes[ThreadLocalRandom.current().nextInt(specie.genomes.size)].clone()
            child = crossover(g1, g2)
        } else {
            child = specie.genomes[ThreadLocalRandom.current().nextInt(specie.genomes.size)].clone()
        }

        child.mutate()

        return child
    }

    fun hasFitnessBeenMeasured(): Boolean {
        val species = species[currentSpecies]
        val genome = species.genomes[currentGenome]

        return genome.fitness != Int.MIN_VALUE
    }
}