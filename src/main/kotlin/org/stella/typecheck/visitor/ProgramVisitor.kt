package org.stella.typecheck.visitor

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaExtension
import org.stella.typecheck.StellaType
import org.stella.typecheck.TypeValidationException
import org.syntax.stella.Absyn.*

class ProgramVisitor : Program.Visitor<Unit, Unit> {

    private val defaultExtensions = hashSetOf(
        StellaExtension.SUM_TYPES,
        StellaExtension.LET_BINDINGS,
    )

    private fun parseExtensions(data: List<Extension>) : Set<StellaExtension> {
        val extensionNames = data.flatMap {
            it as AnExtension
            it.listextensionname_
        }
        val result = hashSetOf<StellaExtension>()
        for (extension in extensionNames) {
            val parsed = StellaExtension.values().find { extension == "#${it.extensionName}" }
            if (parsed == null) {
                TypeValidationException.errorUnknownExtension(extension)
            }
            result += parsed
        }

        return result + defaultExtensions
    }

    override fun visit(p: AProgram, arg: Unit) {
        val functions = hashMapOf<String, StellaType>()
        val extensions = parseExtensions(p.listextension_)
        val exceptionType = inferExceptionType(p.listdecl_)
        val context = FunctionContext(
            functions,
            null,
            FunctionContext.PersistentContext(
                exceptionType,
                extensions,
            ),
        )
        for (decl in p.listdecl_) {
            if (decl !is DeclFun) continue
            val argTypes = decl.listparamdecl_.map { StellaType.fromAst((it as AParamDecl).type_, context) }
            val retType =
                if (decl.returntype_ is SomeReturnType) StellaType.fromAst(
                    decl.returntype_.type_,
                    context
                ) else StellaType.Unit
            functions[decl.stellaident_] = StellaType.Fun(argTypes, retType)
        }
        for (decl in p.listdecl_) {
            when (decl) {
                is DeclFun -> visit(decl, context)
            }
        }

        context.persistent.reconstruction.validate()
    }

    private sealed class ExceptionType(val type: StellaType) {
        class OpenVariant(val t: StellaType.Variant) : ExceptionType(t)
        class Specific(val t: StellaType) : ExceptionType(t)
    }

    private fun inferExceptionType(declarations: List<Decl>): StellaType? {
        val emptyContext = FunctionContext(
            emptyMap(), null, FunctionContext.PersistentContext(
                null, emptySet()
            )
        )
        var r: ExceptionType? = null
        for (decl in declarations) {
            if (decl is DeclExceptionType) {
                r = ExceptionType.Specific(StellaType.fromAst(decl.type_, emptyContext))
            }
            if (decl is DeclExceptionVariant) {
                val tt = if (r !is ExceptionType.OpenVariant) {
                    listOf(decl.stellaident_ to StellaType.fromAst(decl.type_, emptyContext))
                } else {
                    r.t.members + listOf(decl.stellaident_ to StellaType.fromAst(decl.type_, emptyContext))
                }
                r = ExceptionType.OpenVariant(StellaType.Variant(tt))
            }
        }

        return r?.type
    }

    fun visit(p: DeclFun, arg: FunctionContext) {
        tcAssert(p.listparamdecl_.size.let { it == 0 || it == 1 }, "function can accept only one or zero args")
        val newVars = p.listparamdecl_.associate {
            it as AParamDecl
            it.stellaident_ to StellaType.Companion.fromAst(it.type_, arg)
        }

        val expectedType =
            if (p.returntype_ is SomeReturnType) StellaType.fromAst(p.returntype_.type_, arg) else StellaType.Unit
        val context = arg.enterFunction(newVars, expectedType)

        val visitor = ExprVisitor()

        val r = p.expr_.accept(visitor, context)

        arg.cmpTypesOrAddConstraint(p.expr_, expectedType, r)
    }

    private fun tcAssert(condition: Boolean, msg: String) {
        if (!condition) error(msg)
    }
}