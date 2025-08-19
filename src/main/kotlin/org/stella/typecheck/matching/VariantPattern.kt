package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaType
import org.stella.typecheck.TypeValidationException

class VariantPattern(
    val name: String,
    val pattern: PatternModel?
) : PatternModel() {

    override fun checkConforms(context: FunctionContext, type: StellaType) {
        TODO("Not yet implemented")
    }

    private fun calcInnerType(context: FunctionContext, type: StellaType): StellaType? {
        val t = if (type is StellaType.Auto) {
            val tt = StellaType.Variant(
                mapOf(
                    name to pattern?.let { context.persistent.reconstruction.atom() }
                )
            )
            context.persistent.reconstruction.applyConstraint(type, tt)
            tt
        } else {
            type
        }
        if (t !is StellaType.Variant) {
            TypeValidationException.errorUnexpectedTypeForExpression(
                null,
                StellaType.Variant(mapOf(name to StellaType.Unknown)),
                t
            )
        }

        if (name !in t.members) {
            TypeValidationException.errorUnexpectedVariantLabel(name)
        }
        val entry = t.members[name]

        return entry
    }

    override fun createBindings(
        context: FunctionContext,
        type: StellaType
    ): List<Pair<String, StellaType>> {
        val t = calcInnerType(context, type)
        if (t != null && pattern == null) {
            TypeValidationException.errorUnexpectedNullaryVariantPattern(name)
        }
        if (t == null && pattern != null) {
            TypeValidationException.errorUnexpectedNonNullaryVariantPattern(name)
        }
        return pattern?.createBindings(context, t!!) ?: emptyList()
    }
}