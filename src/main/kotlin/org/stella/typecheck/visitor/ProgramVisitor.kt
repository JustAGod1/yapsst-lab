package org.stella.typecheck.visitor

import org.stella.typecheck.FunctionTypeContext
import org.stella.typecheck.StellaType
import org.stella.typecheck.TypeValidationException
import org.syntax.stella.Absyn.*

class ProgramVisitor : Program.Visitor<Unit, Unit> {

    override fun visit(p: AProgram, arg: Unit) {
        val functions = hashMapOf<String, StellaType>()
        for (decl in p.listdecl_) {
            if (decl !is DeclFun) continue
            val argTypes = decl.listparamdecl_.map { StellaType.fromAst((it as AParamDecl).type_) }
            val retType = if (decl.returntype_ is SomeReturnType) StellaType.fromAst(decl.returntype_.type_) else StellaType.Unit
            functions[decl.stellaident_] = StellaType.Fun(argTypes, retType)
        }
        for (decl in p.listdecl_) {
            when (decl) {
                is DeclFun -> visit(decl, FunctionTypeContext(functions, null))
                else -> error("decl with type ${decl.javaClass} is not supported")
            }
        }
    }

    fun visit(p: DeclFun, arg: FunctionTypeContext) {
        tcAssert(p.listparamdecl_.size.let { it == 0 || it == 1 }, "function can accept only one or zero args")
        val newVars = p.listparamdecl_.associate {
            it as AParamDecl
            it.stellaident_ to StellaType.Companion.fromAst(it.type_)
        }

        val expectedType = if (p.returntype_ is SomeReturnType) StellaType.fromAst(p.returntype_.type_) else StellaType.Unit
        val context = arg.enterFunction(newVars, expectedType)

        val visitor = ExprVisitor()

        val r = p.expr_.accept(visitor, context)

        if (r != expectedType) {
            TypeValidationException.errorUnexpectedTypeForExpression(p.expr_, expectedType, r)
        }
    }

    private fun tcAssert(condition: Boolean, msg: String) {
        if (!condition) error(msg)
    }
}