package org.stella.typecheck

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
            TypeValidationException.errorExtensionIsDisabled(extension.extensionName)
        }
    }


    fun cmpTypesOrAddConstraint(expr: Expr, expected: StellaType, actual: StellaType) {
        if (expected == actual) return
        if (hasExtension(StellaExtension.STRUCTURAL_SUBTYPING) && expected.isAssignableFrom(actual))
            return

        if (!expected.hasAuto && !actual.hasAuto) {
            TypeValidationException.errorUnexpectedTypeForExpression(expr, expected, actual)
        }

        persistent.reconstruction.applyConstraint(actual, expected)
    }

}