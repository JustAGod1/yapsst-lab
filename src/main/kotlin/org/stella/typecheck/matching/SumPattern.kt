package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaType
import org.stella.typecheck.TypeValidationException

class SumPattern(
    val inner: PatternModel,
    val left: Boolean
) : PatternModel() {

    override fun checkConforms(context: FunctionContext, type: StellaType) {
        val t = calcType(context, type)
        return if (left) {
            inner.checkConforms(context, t.a)
        } else {
            inner.checkConforms(context, t.b)
        }
    }

    private fun calcType(context: FunctionContext, type: StellaType): StellaType.Sum {
        val t = if (type is StellaType.Auto) {
            val l = context.persistent.reconstruction.atom()
            val r = context.persistent.reconstruction.atom()
            val tt = StellaType.Sum(l, r)
            context.persistent.reconstruction.applyConstraint(type, tt)
            tt
        } else {
            type
        }
        if (t !is StellaType.Sum) {
            TypeValidationException.errorUnexpectedPatternForType(
                StellaType.Sum(StellaType.Unknown, StellaType.Unknown),
                t
            )
        }

        return t
    }

    override fun createBindings(
        context: FunctionContext,
        type: StellaType
    ): List<Pair<String, StellaType>> {
        val t = calcType(context, type)
        return if (left) {
            inner.createBindings(context, t.a)
        } else {
            inner.createBindings(context, t.b)
        }
    }
}