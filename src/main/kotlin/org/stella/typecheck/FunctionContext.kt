package org.stella.typecheck

import org.stella.typecheck.matching.MatchingSlice
import org.syntax.stella.Absyn.Expr
import org.syntax.stella.Absyn.TypeAuto

class FunctionContext(
    val variables: Map<String, StellaType>,
    val expectedType: StellaType?,
    val persistent: PersistentContext
) {

    val exceptionType: StellaType?
        get() = persistent.exceptionType

    class PersistentContext(
        val exceptionType: StellaType?,
        val extensions: Set<StellaExtension>,
    ) {
        val postponedMatches = arrayListOf<Pair<StellaType, MatchingSlice>>()
        val reconstruction = ReconstructionContext()
    }

    fun withVariables(variables: Map<String, StellaType>) =
        FunctionContext(
            this.variables + variables,
            expectedType,
            persistent
        )

    fun withExpectedType(expectedType: StellaType?) = FunctionContext(
        this.variables,
        expectedType,
        persistent
    )

    fun enterFunction(variables: Map<String, StellaType>, returnType: StellaType?) = FunctionContext(
        this.variables + variables,
        returnType,
        persistent
    )


    fun hasExtension(extension: StellaExtension): Boolean {
        return extension in this.persistent.extensions
    }

    fun checkExtension(extension: StellaExtension) {
        if (!hasExtension(extension)) {
            TypeValidationException.errorExtensionIsDisabled(extension.extensionName.first())
        }
    }


    fun cmpTypesOrAddConstraint(expr: Expr?, expected: StellaType, actual: StellaType) {
        if (expected == actual) return

        if (hasExtension(StellaExtension.STRUCTURAL_SUBTYPING)) {
            if (expected.isAssignableFrom(actual)) return
            else TypeValidationException.errorUnexpectedSubtype()
        }

        if (!expected.hasAuto && !actual.hasAuto) {
            if (actual.javaClass != expected.javaClass) {
                if (actual is StellaType.Fun) {
                    TypeValidationException.errorUnexpectedLambda()
                }
                if (actual is StellaType.Tuple) {
                    TypeValidationException.errorUnexpectedTuple()
                }
                if (actual is StellaType.Record) {
                    TypeValidationException.errorUnexpectedRecord()
                }
                if (actual is StellaType.List) {
                    TypeValidationException.errorUnexpectedList()
                }
                if (actual is StellaType.Sum) {
                    TypeValidationException.errorUnexpectedInjection()
                }
            }
            TypeValidationException.errorUnexpectedTypeForExpression(expr, expected, actual)
        }

        persistent.reconstruction.applyConstraint(actual, expected)
    }

}