package org.stella.typecheck

import org.stella.typecheck.matching.MatchingSlice
import org.syntax.stella.Absyn.Expr
import org.syntax.stella.Absyn.TypeVar

class FunctionContext private constructor(
    val variables: Map<String, StellaType>,
    val expectedType: StellaType?,
    private val uContext: UniversalTypesContext?,
    val persistent: PersistentContext,
) {

    constructor(
        variables: Map<String, StellaType>,
        expectedType: StellaType?,
        persistent: PersistentContext
    ) : this(variables, expectedType, null, persistent)

    val exceptionType: StellaType?
        get() = persistent.exceptionType

    class PersistentContext(
        val exceptionType: StellaType?,
        val extensions: Set<StellaExtension>,
    ) {
        val postponedMatches = arrayListOf<Pair<StellaType, MatchingSlice>>()
        val reconstruction = ReconstructionContext()
    }

    fun withTypeVariables(typeVariables: Set<String>, owner: Any) =
        FunctionContext(
            variables,
            expectedType,
            UniversalTypesContext(typeVariables, uContext, owner),
            persistent
        )

    fun withVariables(variables: Map<String, StellaType>) =
        FunctionContext(
            this.variables + variables,
            expectedType,
            uContext,
            persistent
        )

    fun withExpectedType(expectedType: StellaType?) =
        FunctionContext(
            this.variables,
            expectedType,
            uContext,
            persistent
        )

    fun enterFunction(variables: Map<String, StellaType>, returnType: StellaType?) =
        FunctionContext(
            this.variables + variables,
            returnType,
            uContext,
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


    fun cmpTypesOrAddConstraint(
        expr: Expr?,
        expected: StellaType,
        actual: StellaType,
        code: StellaExceptionCode = StellaExceptionCode.ERROR_UNEXPECTED_TYPE_FOR_EXPRESSION
    ) {
        if (expected == actual) return

        if (hasExtension(StellaExtension.STRUCTURAL_SUBTYPING)) {
            return expected.checkAssignableFrom(actual)
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
                if (actual is StellaType.Reference) {
                    TypeValidationException.errorUnexpectedReference()
                }
            }
            if (actual is StellaType.Fun && expected is StellaType.Fun) {
                if (actual.args.size != expected.args.size)
                    TypeValidationException.errorUnexpectedNumberOfParametersInLambda()
            }
            TypeValidationException.make(
                code,
                """
                    Expr: $expr
                    Expected: $expected
                    Actual: $actual
                """.trimIndent()
            )
        }

        persistent.reconstruction.applyConstraint(actual, expected)
    }

    fun createMapping(name: String): StellaType.Var {
        return uContext?.createMapping(name) ?: TypeValidationException.errorUndefinedTypeVariable(name)
    }

    fun checkMapping(o: StellaType.Var): StellaType.Var? {
        return uContext?.checkMapping(o)
    }

    private class UniversalTypesContext(
        val names: Set<String>,
        val parent: UniversalTypesContext?,
        val owner: Any,
    ) {
        val mappings = hashMapOf<String, StellaType.Var>()

        fun createMapping(name: String): StellaType.Var {
            mappings[name]?.let { return it }
            if (name !in names) {
                if (parent != null) return parent.createMapping(name) + names.size
                TypeValidationException.errorUndefinedTypeVariable(name)
            }
            val idx = mappings.values.maxByOrNull { it.idx }?.let { it.idx + 1 } ?: 0
            val n = StellaType.Var(name, owner, idx)
            mappings[name] = n
            return n
        }

        fun checkMapping(v: StellaType.Var): StellaType.Var? {
            return if (owner === v.owner) return mappings[v.name]
            else parent?.checkMapping(v)?.let { it + names.size }
        }
    }

}