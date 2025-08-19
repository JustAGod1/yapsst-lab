package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaType
import org.stella.typecheck.TypeValidationException

class TuplePattern(
    val patterns: List<PatternModel>
) : PatternModel() {

    override fun checkConforms(context: FunctionContext, type: StellaType) {
        val t = calcType(context, type)

        for (i in patterns.indices) {
            val my = patterns[i]
            val that = t.members[i]

            my.checkConforms(context, that)
        }
    }

    private fun calcType(context: FunctionContext, type: StellaType): StellaType.Tuple {
        val t = if (type is StellaType.Auto) {
            val tt = StellaType.Tuple(patterns.map { context.persistent.reconstruction.atom() })
            context.persistent.reconstruction.applyConstraint(type, tt)
            tt
        } else {
            type
        }
        if (t !is StellaType.Tuple) {
            TypeValidationException.errorUnexpectedTypeForExpression(
                null,
                StellaType.Tuple(patterns.map { StellaType.Unknown }),
                t
            )
        }

        if (t.members.size != patterns.size) TypeValidationException.errorUnexpectedTupleLength()

        return t
    }

    override fun createBindings(
        context: FunctionContext,
        type: StellaType
    ): List<Pair<String, StellaType>> {
        val t = calcType(context, type)

        val result = arrayListOf<Pair<String, StellaType>>()
        for (i in patterns.indices) {
            val my = patterns[i]
            val that = t.members[i]

            result.addAll(my.createBindings(context, that))
        }

        return result
    }
}