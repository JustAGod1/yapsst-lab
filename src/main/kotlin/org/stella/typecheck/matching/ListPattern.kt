package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaType
import org.stella.typecheck.TypeValidationException

class ListPattern(val prefix: List<PatternModel>, val suffix: PatternModel?) : PatternModel() {


    fun lower() : Int? {
        if (suffix is BindingPattern) return prefix.size
        if (suffix is ListPattern) {
            return suffix.lower()?.let { it + prefix.size }
        }
        return null
    }
    fun constant() : Int? {
        if (suffix == null) return prefix.size
        if (suffix is ListPattern) {
            return suffix.constant()?.let { it + prefix.size }
        }
        return null
    }

    override fun checkConforms(context: FunctionContext, type: StellaType) {
        val t = calcType(context, type)
        val inner = t.type

        prefix.forEach { it.checkConforms(context, inner) }

        suffix?.checkConforms(context, t)
    }

    private fun calcType(context: FunctionContext, type: StellaType) : StellaType.List {
        val t = if (type is StellaType.Auto) {
            val tt = StellaType.List(context.persistent.reconstruction.atom())
            context.persistent.reconstruction.applyConstraint(type, tt)
            tt
        } else {
            type
        }
        if (t !is StellaType.List) {
            TypeValidationException.errorUnexpectedTypeForExpression(
                null,
                StellaType.List(StellaType.Unknown),
                t
            )
        }

        return t
    }

    override fun createBindings(context: FunctionContext, type: StellaType): List<Pair<String, StellaType>> {
        val t = calcType(context, type)
        val inner = t.type

        return prefix.flatMap { it.createBindings(context, inner) } +
                (suffix?.createBindings(context, type) ?: emptyList())

    }
}