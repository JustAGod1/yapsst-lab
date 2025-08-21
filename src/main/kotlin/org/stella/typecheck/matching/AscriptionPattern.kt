package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaExceptionCode
import org.stella.typecheck.StellaType

class AscriptionPattern(
    val inner: PatternModel,
    val ascription: StellaType
) : PatternModel() {
    override fun checkConforms(context: FunctionContext, type: StellaType) {
        val t = calcType(context, type)
        inner.checkConforms(context, t)
    }

    override fun createBindings(
        context: FunctionContext,
        type: StellaType
    ): List<Pair<String, StellaType>> {
        val t = calcType(context, type)
        return inner.createBindings(context, t)
    }

    private fun calcType(context: FunctionContext, type: StellaType): StellaType {
        val t = if (type is StellaType.Auto) {
            context.persistent.reconstruction.applyConstraint(type, ascription)
            ascription
        } else  {
            context.cmpTypesOrAddConstraint(
                null,
                ascription,
                type,
                StellaExceptionCode.ERROR_UNEXPECTED_PATTERN_FOR_TYPE
            )
            type
        }


        return t
    }

    override fun createBindingsLetrec(context: FunctionContext): List<Pair<String, StellaType>> {
        return inner.createBindings(context, ascription)
    }

    override fun unwrap(): PatternModel {
        return inner
    }
}