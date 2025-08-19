package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaType

object UnitPattern : PatternModel() {
    override fun checkConforms(context: FunctionContext, type: StellaType) =
        context.cmpTypesOrAddConstraint(null, StellaType.Unit, type)

    override fun createBindings(
        context: FunctionContext,
        type: StellaType
    ): List<Pair<String, StellaType>> = emptyList()

}