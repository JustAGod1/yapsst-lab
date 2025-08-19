package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaType

class BindingPattern(val name: String) : PatternModel() {

    override fun checkConforms(context: FunctionContext, type: StellaType) {}

    override fun createBindings(context: FunctionContext, type: StellaType): List<Pair<String, StellaType>> = listOf(name to type)

}