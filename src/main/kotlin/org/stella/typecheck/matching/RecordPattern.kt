package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaType
import org.stella.typecheck.TypeValidationException

class RecordPattern(
    val entries: List<Pair<String, PatternModel>>
) : PatternModel() {

    init {
        val met = hashSetOf<String>()
        for ((k, _) in entries) {
            if (!met.add(k)) {
                TypeValidationException.errorDuplicateRecordPatternFields(k)
            }
        }
    }

    private fun calcType(context: FunctionContext, type: StellaType): StellaType.Record {
        val t = if (type is StellaType.Auto) {
            val tt = StellaType.Record(entries.associate { (k, _) -> k to context.persistent.reconstruction.atom() })
            context.persistent.reconstruction.applyConstraint(type, tt)
            tt
        } else {
            type
        }
        if (t !is StellaType.Record) {
            TypeValidationException.errorUnexpectedTypeForExpression(
                null,
                StellaType.Record(entries.associate { (k, _) -> k to StellaType.Unknown }),
                t
            )
        }

        return t
    }

    override fun checkConforms(context: FunctionContext, type: StellaType) {
        val t = calcType(context, type)

        for ((k, v) in entries) {
            val that = t.members[k] ?: TypeValidationException.errorMissingRecordFields()
            v.checkConforms(context, that)
        }
    }

    override fun createBindings(
        context: FunctionContext,
        type: StellaType
    ): List<Pair<String, StellaType>> {
        val t = calcType(context, type)

        val result = arrayListOf<Pair<String, StellaType>>()
        for ((k, v) in entries) {
            val that = t.members[k] ?: TypeValidationException.errorMissingRecordFields()
            result.addAll(v.createBindings(context, that))
        }

        return result
    }
}