package org.stella.typecheck

import org.syntax.stella.Absyn.TypeAuto
import java.util.LinkedList

class ReconstructionContext(
) {

    private class IdentityWrapper(val value: TypeAuto) {
        override fun equals(other: Any?): Boolean {
            if (other !is IdentityWrapper) return false
            return this.value === other.value
        }

        override fun hashCode(): Int = System.identityHashCode(value)
    }

    private val autoTypes = hashMapOf<IdentityWrapper, StellaType.Auto>()
    private val atoms = arrayListOf<StellaType.Auto>()
    private val constraints = LinkedList<Pair<StellaType, StellaType>>()
    private val substitutions = hashMapOf<StellaType.Auto, StellaType>()

    fun atom(): StellaType.Auto {
        val newIdx = (atoms.maxByOrNull { it.idx }?.idx ?: 0) + 1
        val a = StellaType.Auto(newIdx)
        atoms.add(a)
        return a
    }

    fun autoMap(t: TypeAuto): StellaType.Auto {
        val wrapper = IdentityWrapper(t)
        if (wrapper in autoTypes) {
            return autoTypes[wrapper]!!
        }
        val a = atom()
        autoTypes[wrapper] = a
        return a
    }

    fun applyConstraint(a: StellaType, b: StellaType) {
        constraints.add(a to b)
    }

    fun substitute(auto: StellaType.Auto): StellaType {
        return substitutions[auto] ?: auto
    }

    fun validate() {
        while (constraints.isNotEmpty()) {
            println("Constraints:")
            for ((a, b) in constraints) {
                println("\t${a.substitute(this)} ==> ${b.substitute(this)}")
            }

            val (a, b) = constraints.removeFirst()
            observeConstraint(a, b)
        }

        for ((a, b) in substitutions) {
            print("\t$a |--> $b")
            if (b.contains(a)) {
                TypeValidationException.errorOccursCheckInfiniteType("$a |--> $b")
            }
        }
    }

    private fun observeConstraint(a: StellaType, b: StellaType) {
        doObserveConstraint(a.substitute(this), b.substitute(this))
    }

    private fun checkConflict(existing: StellaType.Auto, assigning: StellaType) {
        val substituted = substitutions[existing]
        if (substituted != null) {
            constraints += substituted to assigning
        } else {
            substitutions[existing] = assigning
        }
    }

    private fun doObserveConstraint(a: StellaType, b: StellaType) {
        if (a == b) return
        if (a is StellaType.Auto) {
            checkConflict(a, b)
            return
        }
        if (b is StellaType.Auto) {
            checkConflict(b, a)
            return
        }

        when {
            a is StellaType.Record && b is StellaType.Record && a.members.keys != b.members.keys -> {
                for (name in a.members.keys) {
                    doObserveConstraint(a.members[name]!!, b.members[name]!!)
                }
            }

            a is StellaType.Tuple && b is StellaType.Tuple && a.members.size != b.members.size -> {
                for (i in a.members.indices) {
                    doObserveConstraint(a.members[i], b.members[i])
                }
            }

            a is StellaType.List && b is StellaType.List -> {
                doObserveConstraint(a.type, b.type)
            }

            a is StellaType.Reference && b is StellaType.Reference -> {
                doObserveConstraint(a.underlyingType, b.underlyingType)
            }

            a is StellaType.Sum && b is StellaType.Sum -> {
                doObserveConstraint(a.a, b.a)
                doObserveConstraint(a.b, b.b)
            }

            a is StellaType.Variant && b is StellaType.Variant && a.members.keys == b.members.keys -> {
                for (name in a.members.keys) {
                    val at = a.members[name]
                    val bt = b.members[name]
                    if ((at == null) != (bt == null))
                        TypeValidationException.make(StellaExceptionCode.ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION, "Cannot satisfy constraint $a |-> $b")
                    if (at != null) doObserveConstraint(at, bt!!)
                }
            }

            a is StellaType.Fun && b is StellaType.Fun && a.args.size == b.args.size -> {
                doObserveConstraint(a.returnType, b.returnType)
                for (i in a.args.indices) {
                    doObserveConstraint(a.args[i], b.args[i])
                }
            }
            else ->
                TypeValidationException.make(StellaExceptionCode.ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION, "Cannot satisfy constraint $a |-> $b")
        }


    }
}