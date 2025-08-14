package org.stella.typecheck.visitor

import org.stella.typecheck.FunctionTypeContext
import org.stella.typecheck.StellaType
import org.stella.typecheck.TypeValidationException
import org.syntax.stella.Absyn.*

class ProgramVisitor : Program.Visitor<Unit, Unit> {

    override fun visit(p: AProgram, arg: Unit) {
        val functions = hashMapOf<String, StellaType>()
        val extensions = p.listextension_.flatMap { (it as AnExtension).listextensionname_ }.toSet()
        for (decl in p.listdecl_) {
            if (decl !is DeclFun) continue
            val argTypes = decl.listparamdecl_.map { StellaType.fromAst((it as AParamDecl).type_) }
            val retType = if (decl.returntype_ is SomeReturnType) StellaType.fromAst(decl.returntype_.type_) else StellaType.Unit
            functions[decl.stellaident_] = StellaType.Fun(argTypes, retType)
        }
        val exceptionType = inferExceptionType(p.listdecl_)
        val context = FunctionTypeContext(
            exceptionType,
            extensions,
            functions,
            null
        )
        for (decl in p.listdecl_) {
            when (decl) {
                is DeclFun -> visit(decl, context)
            }
        }
    }

    private sealed class ExceptionType(val type: StellaType) {
        class OpenVariant(val t: StellaType.Variant) : ExceptionType(t)
        class Specific(val t: StellaType) : ExceptionType(t)
    }

    private fun inferExceptionType(declarations: List<Decl>): StellaType? {
        var r: ExceptionType? = null
        for (decl in declarations) {
            if (decl is DeclExceptionType) {
                r = ExceptionType.Specific(StellaType.fromAst(decl.type_))
            }
            if (decl is DeclExceptionVariant) {
                val tt = if (r !is ExceptionType.OpenVariant) {
                    listOf(decl.stellaident_ to StellaType.fromAst(decl.type_))
                } else {
                    r.t.members + listOf(decl.stellaident_ to StellaType.fromAst(decl.type_))
                }
                r = ExceptionType.OpenVariant(StellaType.Variant(tt))
            }
        }

        return r?.type
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