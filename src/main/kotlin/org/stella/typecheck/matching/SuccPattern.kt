package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaType

class SuccPattern(
    val inner: PatternModel,
) : PatternModel() {

    fun lower() : Int? {
        if (inner is SuccPattern) {
            return inner.lower()?.let { it + 1}
        }
        if (inner is BindingPattern) {
            return 1
        }
        return null
    }
    fun constant() : Int? {
        if (inner is SuccPattern) {
            return inner.constant()?.let { it + 1}
        }
        if (inner is NatPattern) {
            return inner.value + 1
        }
        return null
    }

    override fun checkConforms(context: FunctionContext, type: StellaType) {
        context.cmpTypesOrAddConstraint(null, StellaType.Nat, type)
        inner.checkConforms(context, StellaType.Nat)
    }

    override fun createBindings(
        context: FunctionContext,
        type: StellaType
    ): List<Pair<String, StellaType>> {
        context.cmpTypesOrAddConstraint(null, StellaType.Nat, type)

        return inner.createBindings(context, StellaType.Nat)
    }
}