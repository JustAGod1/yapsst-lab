package org.stella.typecheck

import org.syntax.stella.Absyn.Expr

enum class ExceptionCode {
    ERROR_MISSING_MAIN,
    ERROR_UNDEFINED_VARIABLE,
    ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION,
    ERROR_NOT_A_FUNCTION,
    ERROR_NOT_A_TUPLE,
    ERROR_NOT_A_RECORD,
    ERROR_NOT_A_LIST,
    ERROR_UNEXPECTED_LAMBDA,
    ERROR_UNEXPECTED_TYPE_FOR_PARAMETER,
    ERROR_UNEXPECTED_TUPLE,
    ERROR_UNEXPECTED_RECORD,
    ERROR_UNEXPECTED_VARIANT,
    ERROR_UNEXPECTED_LIST,
    ERROR_UNEXPECTED_INJECTION,
    ERROR_MISSING_RECORD_FIELDS,
    ERROR_UNEXPECTED_RECORD_FIELDS,
    ERROR_UNEXPECTED_FIELD_ACCESS,
    ERROR_UNEXPECTED_VARIANT_LABEL,
    ERROR_TUPLE_INDEX_OUT_OF_BOUNDS,
    ERROR_UNEXPECTED_TUPLE_LENGTH,
    ERROR_AMBIGUOUS_SUM_TYPE,
    ERROR_AMBIGUOUS_VARIANT_TYPE,
    ERROR_AMBIGUOUS_LIST,
    ERROR_ILLEGAL_EMPTY_MATCHING,
    ERROR_NONEXHAUSTIVE_MATCH_PATTERNS,
    ERROR_UNEXPECTED_PATTERN_FOR_TYPE,
    ERROR_DUPLICATE_RECORD_FIELDS,
    ERROR_DUPLICATE_RECORD_TYPE_FIELDS,
    ERROR_DUPLICATE_VARIANT_TYPE_FIELDS
}

class TypeValidationException private constructor(
    val code: ExceptionCode,
    message: String
) : Exception(code.name + "\n" + message) {
    companion object {
        private fun make(code: ExceptionCode, msg: String?): Nothing {
            throw TypeValidationException(
                code, (msg ?: "no message")
            )
        }

        fun errorMissingMain(): Nothing = make(ExceptionCode.ERROR_MISSING_MAIN, null)
        fun errorUndefinedVariable(): Nothing = make(ExceptionCode.ERROR_UNDEFINED_VARIABLE, null)
        fun errorUnexpectedTypeForExpression(expr: Expr, expected: StellaType, actual: StellaType): Nothing =
            make(ExceptionCode.ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION,
                """Expression: $expr
                    |Expected type: $expected
                    |Actual type: $actual
                """.trimMargin())

        fun errorNotAFunction(): Nothing = make(ExceptionCode.ERROR_NOT_A_FUNCTION, null)
        fun errorNotATuple(): Nothing = make(ExceptionCode.ERROR_NOT_A_TUPLE, null)
        fun errorNotARecord(): Nothing = make(ExceptionCode.ERROR_NOT_A_RECORD, null)
        fun errorNotAList(): Nothing = make(ExceptionCode.ERROR_NOT_A_LIST, null)
        fun errorUnexpectedLambda(): Nothing = make(ExceptionCode.ERROR_UNEXPECTED_LAMBDA, null)
        fun errorUnexpectedTypeForParameter(): Nothing = make(ExceptionCode.ERROR_UNEXPECTED_TYPE_FOR_PARAMETER, null)
        fun errorUnexpectedTuple(): Nothing = make(ExceptionCode.ERROR_UNEXPECTED_TUPLE, null)
        fun errorUnexpectedRecord(): Nothing = make(ExceptionCode.ERROR_UNEXPECTED_RECORD, null)
        fun errorUnexpectedVariant(): Nothing = make(ExceptionCode.ERROR_UNEXPECTED_VARIANT, null)
        fun errorUnexpectedList(): Nothing = make(ExceptionCode.ERROR_UNEXPECTED_LIST, null)
        fun errorUnexpectedInjection(): Nothing = make(ExceptionCode.ERROR_UNEXPECTED_INJECTION, null)
        fun errorMissingRecordFields(): Nothing = make(ExceptionCode.ERROR_MISSING_RECORD_FIELDS, null)
        fun errorUnexpectedRecordFields(): Nothing = make(ExceptionCode.ERROR_UNEXPECTED_RECORD_FIELDS, null)
        fun errorUnexpectedFieldAccess(): Nothing = make(ExceptionCode.ERROR_UNEXPECTED_FIELD_ACCESS, null)
        fun errorUnexpectedVariantLabel(): Nothing = make(ExceptionCode.ERROR_UNEXPECTED_VARIANT_LABEL, null)
        fun errorTupleIndexOutOfBounds(): Nothing = make(ExceptionCode.ERROR_TUPLE_INDEX_OUT_OF_BOUNDS, null)
        fun errorUnexpectedTupleLength(): Nothing = make(ExceptionCode.ERROR_UNEXPECTED_TUPLE_LENGTH, null)
        fun errorAmbiguousSumType(): Nothing = make(ExceptionCode.ERROR_AMBIGUOUS_SUM_TYPE, null)
        fun errorAmbiguousVariantType(): Nothing = make(ExceptionCode.ERROR_AMBIGUOUS_VARIANT_TYPE, null)
        fun errorAmbiguousList(): Nothing = make(ExceptionCode.ERROR_AMBIGUOUS_LIST, null)
        fun errorIllegalEmptyMatching(): Nothing = make(ExceptionCode.ERROR_ILLEGAL_EMPTY_MATCHING, null)
        fun errorNonexhaustiveMatchPatterns(): Nothing = make(ExceptionCode.ERROR_NONEXHAUSTIVE_MATCH_PATTERNS, null)
        fun errorUnexpectedPatternForType(): Nothing = make(ExceptionCode.ERROR_UNEXPECTED_PATTERN_FOR_TYPE, null)
        fun errorDuplicateRecordFields(): Nothing = make(ExceptionCode.ERROR_DUPLICATE_RECORD_FIELDS, null)
        fun errorDuplicateRecordTypeFields(): Nothing = make(ExceptionCode.ERROR_DUPLICATE_RECORD_TYPE_FIELDS, null)
        fun errorDuplicateVariantTypeFields(): Nothing = make(ExceptionCode.ERROR_DUPLICATE_VARIANT_TYPE_FIELDS, null)
    }
}

