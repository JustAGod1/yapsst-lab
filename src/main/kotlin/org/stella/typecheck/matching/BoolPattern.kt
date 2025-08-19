package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaType

class BoolPattern(val value: Boolean) : PatternModel() {
    override fun checkConforms(context: FunctionContext, type: StellaType) {
        context.cmpTypesOrAddConstraint(null, type, StellaType.Bool)
    }

    override fun createBindings(
        context: FunctionContext,
        type: StellaType
    ): List<Pair<String, StellaType>> = emptyList()


}