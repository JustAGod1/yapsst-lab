package org.stella.typecheck

import org.syntax.stella.Absyn.TypeAuto
import kotlin.collections.get
import kotlin.text.set

class ReconstructionContext(
) {

    private val autoTypes = hashMapOf<TypeAuto, StellaType.Auto>()
    private val atoms = hashSetOf<StellaType.Auto>()
    private val constraints = hashSetOf<Pair<StellaType, StellaType>>()
    private val substitutions = hashMapOf<StellaType.Auto, StellaType>()

    fun atom(): StellaType.Auto {
        val newIdx = (atoms.maxByOrNull { it.idx }?.idx ?: 0) + 1
        val a = StellaType.Auto(newIdx)
        return a
    }

    fun autoMap(t: TypeAuto): StellaType.Auto {
        if (t in autoTypes) {
            return autoTypes[t]!!
        }
        val a = atom()
        autoTypes[t] = a
        return a
    }

    fun applyConstraint(a: StellaType, b: StellaType) {
        constraints.add(a to b)
    }

    fun substitute(auto: StellaType.Auto): StellaType {
        return substitutions[auto] ?: auto
    }

    fun validate() {
        for ((a, b) in constraints) {
            observeConstraint(a, b)
        }
    }

    private fun observeConstraint(a: StellaType, b: StellaType) {
        doObserveConstraint(a.substitute(this), b.substitute(this))
    }

    private fun doObserveConstraint(a: StellaType, b: StellaType) {
        if (a is StellaType.Auto) {
            substitutions[a] = b
            return
        }
        if (b is StellaType.Auto) {
            substitutions[b] = a
            return
        }

        TypeValidationException.errorOccursCheckInfiniteType("Cannot satisfy constraint $a |-> $b")
    }
}