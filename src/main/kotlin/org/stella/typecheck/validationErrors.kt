package org.stella.typecheck

import org.syntax.stella.Absyn.Expr

enum class StellaExceptionCode {
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
    ERROR_DUPLICATE_VARIANT_TYPE_FIELDS,

    // Second Stage
    ERROR_EXCEPTION_TYPE_NOT_DECLARED,
    ERROR_AMBIGUOUS_THROW_TYPE,
    ERROR_AMBIGUOUS_REFERENCE_TYPE,
    ERROR_AMBIGUOUS_PANIC_TYPE,
    ERROR_NOT_A_REFERENCE,
    ERROR_UNEXPECTED_MEMORY_ADDRESS,
    ERROR_UNEXPECTED_SUBTYPE,

    // Extra
    ERROR_EXTENSION_IS_DISABLED,
}

class TypeValidationException private constructor(
    val code: StellaExceptionCode,
    message: String
) : Exception(code.name + "\n" + message) {
    companion object {
        private fun make(code: StellaExceptionCode, msg: String?): Nothing {
            throw TypeValidationException(
                code, (msg ?: "no message")
            )
        }

        // Extra
        fun errorExtensionIsDisabled(wantedExtension: String): Nothing = make(
            StellaExceptionCode.ERROR_EXTENSION_IS_DISABLED,
            "$wantedExtension is not enabled"
        )

        // Stage 1
        fun errorMissingMain(): Nothing = make(StellaExceptionCode.ERROR_MISSING_MAIN, null)
        fun errorUndefinedVariable(): Nothing = make(StellaExceptionCode.ERROR_UNDEFINED_VARIABLE, null)
        fun errorUnexpectedTypeForExpression(expr: Expr, expected: StellaType, actual: StellaType): Nothing =
            make(StellaExceptionCode.ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION,
                """Expression: $expr
                    |Expected type: $expected
                    |Actual type: $actual
                """.trimMargin())

        fun errorNotAFunction(): Nothing = make(StellaExceptionCode.ERROR_NOT_A_FUNCTION, null)
        fun errorNotATuple(): Nothing = make(StellaExceptionCode.ERROR_NOT_A_TUPLE, null)
        fun errorNotARecord(): Nothing = make(StellaExceptionCode.ERROR_NOT_A_RECORD, null)
        fun errorNotAList(): Nothing = make(StellaExceptionCode.ERROR_NOT_A_LIST, null)
        fun errorUnexpectedLambda(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_LAMBDA, null)
        fun errorUnexpectedTypeForParameter(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_TYPE_FOR_PARAMETER, null)
        fun errorUnexpectedTuple(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_TUPLE, null)
        fun errorUnexpectedRecord(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_RECORD, null)
        fun errorUnexpectedVariant(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_VARIANT, null)
        fun errorUnexpectedList(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_LIST, null)
        fun errorUnexpectedInjection(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_INJECTION, null)
        fun errorMissingRecordFields(): Nothing = make(StellaExceptionCode.ERROR_MISSING_RECORD_FIELDS, null)
        fun errorUnexpectedRecordFields(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_RECORD_FIELDS, null)
        fun errorUnexpectedFieldAccess(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_FIELD_ACCESS, null)
        fun errorUnexpectedVariantLabel(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_VARIANT_LABEL, null)
        fun errorTupleIndexOutOfBounds(): Nothing = make(StellaExceptionCode.ERROR_TUPLE_INDEX_OUT_OF_BOUNDS, null)
        fun errorUnexpectedTupleLength(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_TUPLE_LENGTH, null)
        fun errorAmbiguousSumType(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_SUM_TYPE, null)
        fun errorAmbiguousVariantType(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_VARIANT_TYPE, null)
        fun errorAmbiguousList(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_LIST, null)
        fun errorIllegalEmptyMatching(): Nothing = make(StellaExceptionCode.ERROR_ILLEGAL_EMPTY_MATCHING, null)
        fun errorNonexhaustiveMatchPatterns(): Nothing = make(StellaExceptionCode.ERROR_NONEXHAUSTIVE_MATCH_PATTERNS, null)
        fun errorUnexpectedPatternForType(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_PATTERN_FOR_TYPE, null)
        fun errorDuplicateRecordFields(): Nothing = make(StellaExceptionCode.ERROR_DUPLICATE_RECORD_FIELDS, null)
        fun errorDuplicateRecordTypeFields(): Nothing = make(StellaExceptionCode.ERROR_DUPLICATE_RECORD_TYPE_FIELDS, null)
        fun errorDuplicateVariantTypeFields(): Nothing = make(StellaExceptionCode.ERROR_DUPLICATE_VARIANT_TYPE_FIELDS, null)

        // Stage 2
        fun errorExceptionTypeNotDeclared(): Nothing = make(StellaExceptionCode.ERROR_EXCEPTION_TYPE_NOT_DECLARED, null)
        fun errorAmbiguousThrowType(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_THROW_TYPE, null)
        fun errorAmbiguousReferenceType(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_REFERENCE_TYPE, null)
        fun errorAmbiguousPanicType(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_PANIC_TYPE, null)
        fun errorNotAReference(): Nothing = make(StellaExceptionCode.ERROR_NOT_A_REFERENCE, null)
        fun errorUnexpectedMemoryAddress(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_MEMORY_ADDRESS, null)
        fun errorUnexpectedSubtype(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_SUBTYPE, null)
    }
}

