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
    ERROR_DUPLICATE_RECORD_PATTERN_FIELDS,
    ERROR_DUPLICATE_RECORD_TYPE_FIELDS,
    ERROR_DUPLICATE_VARIANT_TYPE_FIELDS,
    ERROR_AMBIGUOUS_PATTERN_TYPE,
    ERROR_UNEXPECTED_DATA_FOR_NULLARY_LABEL,
    ERROR_MISSING_DATA_FOR_LABEL,
    ERROR_UNEXPECTED_NON_NULLARY_VARIANT_PATTERN,
    ERROR_UNEXPECTED_NULLARY_VARIANT_PATTERN,
    ERROR_INCORRECT_ARITY_OF_MAIN,
    ERROR_INCORRECT_NUMBER_OF_ARGUMENTS,
    ERROR_UNEXPECTED_NUMBER_OF_PARAMETERS_IN_LAMBDA,


    // Second Stage
    ERROR_EXCEPTION_TYPE_NOT_DECLARED,
    ERROR_AMBIGUOUS_THROW_TYPE,
    ERROR_AMBIGUOUS_REFERENCE_TYPE,
    ERROR_AMBIGUOUS_PANIC_TYPE,
    ERROR_NOT_A_REFERENCE,
    ERROR_UNEXPECTED_MEMORY_ADDRESS,
    ERROR_UNEXPECTED_SUBTYPE,
    ERROR_UNEXPECTED_REFERENCE,

    // Third Stage
    ERROR_OCCURS_CHECK_INFINITE_TYPE,
    ERROR_NOT_A_GENERIC_FUNCTION,
    ERROR_INCORRECT_NUMBER_OF_TYPE_ARGUMENTS,
    ERROR_UNDEFINED_TYPE_VARIABLE,

    // Extra
    ERROR_EXTENSION_IS_DISABLED,
    ERROR_UNKNOWN_EXTENSION,
}

class TypeValidationException private constructor(
    val code: StellaExceptionCode,
    message: String
) : Exception(code.name + "\n" + message) {
    companion object {
        fun make(code: StellaExceptionCode, msg: String?): Nothing {
            throw TypeValidationException(
                code, (msg ?: "no message")
            )
        }

        // Extra
        fun errorExtensionIsDisabled(wantedExtension: String): Nothing = make(
            StellaExceptionCode.ERROR_EXTENSION_IS_DISABLED,
            "$wantedExtension is not enabled"
        )
        fun errorUnknownExtension(extensionName: String): Nothing = make(
            StellaExceptionCode.ERROR_UNKNOWN_EXTENSION,
            "$extensionName is not known"
        )

        // Stage 1
        fun errorMissingMain(): Nothing = make(StellaExceptionCode.ERROR_MISSING_MAIN, null)
        fun errorUndefinedVariable(name: String): Nothing = make(StellaExceptionCode.ERROR_UNDEFINED_VARIABLE, name)
        fun errorUnexpectedTypeForExpression(expr: Expr?, expected: StellaType, actual: StellaType): Nothing =
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
        fun errorMissingRecordFields(name: String): Nothing = make(StellaExceptionCode.ERROR_MISSING_RECORD_FIELDS, name)
        fun errorUnexpectedRecordFields(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_RECORD_FIELDS, null)
        fun errorUnexpectedFieldAccess(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_FIELD_ACCESS, null)
        fun errorUnexpectedVariantLabel(labelName: String): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_VARIANT_LABEL, "Unexpected label name: $labelName")
        fun errorTupleIndexOutOfBounds(): Nothing = make(StellaExceptionCode.ERROR_TUPLE_INDEX_OUT_OF_BOUNDS, null)
        fun errorUnexpectedTupleLength(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_TUPLE_LENGTH, null)
        fun errorAmbiguousSumType(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_SUM_TYPE, null)
        fun errorAmbiguousVariantType(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_VARIANT_TYPE, null)
        fun errorAmbiguousPatternType(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_PATTERN_TYPE, null)
        fun errorAmbiguousList(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_LIST, null)
        fun errorIllegalEmptyMatching(): Nothing = make(StellaExceptionCode.ERROR_ILLEGAL_EMPTY_MATCHING, null)
        fun errorNonexhaustiveMatchPatterns(): Nothing = make(StellaExceptionCode.ERROR_NONEXHAUSTIVE_MATCH_PATTERNS, null)
        fun errorUnexpectedPatternForType(expected: StellaType, actual: StellaType): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_PATTERN_FOR_TYPE, "Expected: $expected\nActual: $actual")
        fun errorDuplicateRecordPatternFields(duplicated: String): Nothing = make(StellaExceptionCode.ERROR_DUPLICATE_RECORD_PATTERN_FIELDS, "Duplicated: $duplicated")
        fun errorDuplicateRecordFields(): Nothing = make(StellaExceptionCode.ERROR_DUPLICATE_RECORD_FIELDS, null)
        fun errorDuplicateRecordTypeFields(): Nothing = make(StellaExceptionCode.ERROR_DUPLICATE_RECORD_TYPE_FIELDS, null)
        fun errorDuplicateVariantTypeFields(): Nothing = make(StellaExceptionCode.ERROR_DUPLICATE_VARIANT_TYPE_FIELDS, null)
        fun errorIncorrectArityOfMain(): Nothing = make(StellaExceptionCode.ERROR_INCORRECT_ARITY_OF_MAIN, null)
        fun errorIncorrectNumberOfArguments(): Nothing = make(StellaExceptionCode.ERROR_INCORRECT_NUMBER_OF_ARGUMENTS, null)
        fun errorUnexpectedNumberOfParametersInLambda(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_NUMBER_OF_PARAMETERS_IN_LAMBDA, null)

        fun errorUnexpectedDataForNullaryLabel(name: String): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_DATA_FOR_NULLARY_LABEL, name)
        fun errorMissingDataForLabel(name: String): Nothing = make(StellaExceptionCode.ERROR_MISSING_DATA_FOR_LABEL, name)
        fun errorUnexpectedNonNullaryVariantPattern(name: String): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_NON_NULLARY_VARIANT_PATTERN, name)
        fun errorUnexpectedNullaryVariantPattern(name: String): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_NULLARY_VARIANT_PATTERN, name)

        // Stage 2
        fun errorExceptionTypeNotDeclared(): Nothing = make(StellaExceptionCode.ERROR_EXCEPTION_TYPE_NOT_DECLARED, null)
        fun errorAmbiguousThrowType(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_THROW_TYPE, null)
        fun errorAmbiguousReferenceType(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_REFERENCE_TYPE, null)
        fun errorAmbiguousPanicType(): Nothing = make(StellaExceptionCode.ERROR_AMBIGUOUS_PANIC_TYPE, null)
        fun errorNotAReference(): Nothing = make(StellaExceptionCode.ERROR_NOT_A_REFERENCE, null)
        fun errorUnexpectedMemoryAddress(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_MEMORY_ADDRESS, null)
        fun errorUnexpectedSubtype(expected: StellaType, actual: StellaType): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_SUBTYPE, "Expected: $expected\nActual: $actual")
        fun errorUnexpectedReference(): Nothing = make(StellaExceptionCode.ERROR_UNEXPECTED_REFERENCE, null)

        // Stage 3
        fun errorOccursCheckInfiniteType(msg: String): Nothing = make(StellaExceptionCode.ERROR_OCCURS_CHECK_INFINITE_TYPE, msg)
        fun errorNotAGenericFunction(): Nothing = make(StellaExceptionCode.ERROR_NOT_A_GENERIC_FUNCTION, null)
        fun errorIncorrectNumberOfTypeArguments(): Nothing = make(StellaExceptionCode.ERROR_INCORRECT_NUMBER_OF_TYPE_ARGUMENTS, null)
        fun errorUndefinedTypeVariable(name: String): Nothing = make(StellaExceptionCode.ERROR_UNDEFINED_TYPE_VARIABLE, name)
    }
}

