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

    private fun parseExtensions(data: List<Extension>): Set<StellaExtension> {
        val extensionNames = data.flatMap {
            it as AnExtension
            it.listextensionname_
        }
        val result = hashSetOf<StellaExtension>()
        for (extension in extensionNames) {
            val parsed = StellaExtension.values().find { it.extensionName.any { extension == "#$it" } }
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
        val exceptionType = inferExceptionType(p.listdecl_, extensions)
        val context = FunctionContext(
            functions,
            null,
            FunctionContext.PersistentContext(
                exceptionType,
                extensions,
            ),
        )
        functions += registerFunctions(p.listdecl_, context)

        checkMain(functions)

        for (decl in p.listdecl_) {
            when (decl) {
                is DeclFun -> visit(decl, context)
                is DeclFunGeneric -> visit(decl, context)
            }
        }

        context.persistent.reconstruction.validate()

        for ((type, slice) in context.persistent.postponedMatches) {
            if (!slice.checkExhaustive(context, type.substitute(context.persistent.reconstruction))) {
                TypeValidationException.errorNonexhaustiveMatchPatterns()
            }
        }
    }

    private fun parseFunctionSignature(decl: DeclFun, context: FunctionContext): StellaType.Fun {
        val argTypes =
            decl.listparamdecl_.map { StellaType.fromAst((it as AParamDecl).type_, context) }


        val retType =
            if (decl.returntype_ is SomeReturnType) StellaType.fromAst(
                decl.returntype_.type_,
                context
            ) else StellaType.Unit
        return StellaType.Fun(argTypes, retType)
    }

    private fun parseFunctionSignature(decl: DeclFunGeneric, context: FunctionContext): Pair<StellaType.ForAll, FunctionContext> {
        val names = decl.liststellaident_
        val uContext = context.withTypeVariables(names.toSet(), decl)
        val argTypes =
            decl.listparamdecl_.map { StellaType.fromAst((it as AParamDecl).type_, uContext) }


        val retType =
            if (decl.returntype_ is SomeReturnType) StellaType.fromAst(
                decl.returntype_.type_,
                uContext
            ) else StellaType.Unit
        return StellaType.ForAll(
                names,
                StellaType.Fun(argTypes, retType)
            ) to uContext
    }

    private fun registerFunctions(
        declarations: List<Decl>,
        context: FunctionContext
    ): Map<String, StellaType> {
        val result = hashMapOf<String, StellaType>()
        for (decl in declarations) {
            if (decl !is DeclFun && decl !is DeclFunGeneric) continue

            if (decl is DeclFunGeneric) {
                result[decl.stellaident_] = parseFunctionSignature(decl, context).first
            } else {
                decl as DeclFun
                result[decl.stellaident_] = parseFunctionSignature(decl, context)
            }
        }

        return result
    }


    private fun checkMain(functions: Map<String, StellaType>) {
        val mainFunctionMaybeGeneric = functions["main"] ?: TypeValidationException.errorMissingMain()

        val mainFunction = if (mainFunctionMaybeGeneric is StellaType.ForAll) {
            mainFunctionMaybeGeneric.underlying as StellaType.Fun
        } else {
            mainFunctionMaybeGeneric as StellaType.Fun
        }

        if (mainFunction.args.size != 1) TypeValidationException.errorIncorrectArityOfMain()
    }

    private sealed class ExceptionType(val type: StellaType) {
        class OpenVariant(val t: StellaType.Variant) : ExceptionType(t)
        class Specific(val t: StellaType) : ExceptionType(t)
    }

    private fun inferExceptionType(declarations: List<Decl>, extensions: Set<StellaExtension>): StellaType? {
        val emptyContext = FunctionContext(
            emptyMap(), null, FunctionContext.PersistentContext(
                null, extensions
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
                    r.t.membersList + listOf(decl.stellaident_ to StellaType.fromAst(decl.type_, emptyContext))
                }
                r = ExceptionType.OpenVariant(StellaType.Variant(tt))
            }
        }

        return r?.type
    }

    private fun parseInnerFunctions(context: FunctionContext, declarations: List<Decl>): Map<String, StellaType> {
        val localFunctions = registerFunctions(declarations, context)
        if (localFunctions.isNotEmpty()) {
            context.checkExtension(StellaExtension.NESTED_FUNCTION_DECLARATIONS)
            for (decl in declarations) {
                if (decl is DeclFunGeneric) visit(decl, context)
                if (decl is DeclFun) visit(decl, context)
            }
        }

        return localFunctions
    }

    fun visit(p: DeclFunGeneric, arg: FunctionContext) {
        val (signature, uContext) = parseFunctionSignature(p, arg)
        val f = signature.underlying as StellaType.Fun
        val newVars = p.listparamdecl_.map { (it as AParamDecl).stellaident_ }.zip(f.args).toMap()
        val localFunctions = parseInnerFunctions(arg.withVariables(newVars), p.listdecl_)


        validateFunction(
            uContext,
            localFunctions + newVars,
            p.expr_,
            f
        )
    }

    fun visit(p: DeclFun, arg: FunctionContext) {
        val signature = parseFunctionSignature(p, arg)
        val newVars = p.listparamdecl_.map { (it as AParamDecl).stellaident_ }.zip(signature.args).toMap()
        val localFunctions = parseInnerFunctions(arg.withVariables(newVars), p.listdecl_)

        validateFunction(arg, localFunctions + newVars, p.expr_, signature)
    }

    private fun validateFunction(
        context: FunctionContext,
        args: Map<String, StellaType>,
        body: Expr,
        signature: StellaType.Fun,
    ) {
        val expectedType = signature.returnType

        val context = context.enterFunction(args, expectedType)

        val visitor = ExprVisitor()

        val r = body.accept(visitor, context)

        context.cmpTypesOrAddConstraint(body, expectedType, r)
    }
}