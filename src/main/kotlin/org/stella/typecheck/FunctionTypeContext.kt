package org.stella.typecheck

class FunctionTypeContext(
    val variables: Map<String, StellaType>,
    val expectedType: StellaType?
) {

    fun withVariables(variables: Map<String, StellaType>) = FunctionTypeContext(this.variables + variables, expectedType)
    fun withExpectedType(expectedType: StellaType?) = FunctionTypeContext(this.variables, expectedType)

    fun enterFunction(variables: Map<String, StellaType>, returnType: StellaType?) = FunctionTypeContext(
        this.variables + variables,
        returnType
    )
}