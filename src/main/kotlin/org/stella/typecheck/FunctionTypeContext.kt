package org.stella.typecheck

class FunctionTypeContext(
    val exceptionType: StellaType?,
    val extensions: Set<String>,
    val variables: Map<String, StellaType>,
    val expectedType: StellaType?
) {

    fun withVariables(variables: Map<String, StellaType>) =
        FunctionTypeContext(
            exceptionType,
            extensions,
            this.variables + variables,
            expectedType
        )

    fun withExpectedType(expectedType: StellaType?) = FunctionTypeContext(
        exceptionType,
        extensions,
        this.variables,
        expectedType
    )

    fun enterFunction(variables: Map<String, StellaType>, returnType: StellaType?) = FunctionTypeContext(
        exceptionType,
        extensions,
        this.variables + variables,
        returnType
    )
}