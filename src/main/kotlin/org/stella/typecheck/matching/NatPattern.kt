package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaType

class NatPattern(
    val value: Int
) : PatternModel() {
    override fun checkConforms(context: FunctionContext, type: StellaType) {
        context.cmpTypesOrAddConstraint(null, StellaType.Nat, type)
    }

    override fun createBindings(
        context: FunctionContext,
        type: StellaType
    ): List<Pair<String, StellaType>> {
        context.cmpTypesOrAddConstraint(null, StellaType.Nat, type)
        return emptyList()
    }
}