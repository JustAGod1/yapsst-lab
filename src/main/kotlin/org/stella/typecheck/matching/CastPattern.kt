package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaExceptionCode
import org.stella.typecheck.StellaType
import org.stella.typecheck.TypeValidationException

class CastPattern(
    val inner: PatternModel,
    val narrowType: StellaType
) : PatternModel() {

    private fun checkType(type: StellaType) {
        if (type.isAssignableFrom(narrowType)) {
            TypeValidationException.make(
                StellaExceptionCode.ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION,
                "$type expected to be wider than $narrowType"
            )
        }
    }

    override fun checkConforms(context: FunctionContext, type: StellaType) {
        checkType(type)
        inner.checkConforms(context, narrowType)
    }

    override fun createBindings(
        context: FunctionContext,
        type: StellaType
    ): List<Pair<String, StellaType>> {
        checkType(type)

        return inner.createBindings(context, narrowType)
    }
}