package org.stella.typecheck.visitor

import org.stella.typecheck.FunctionTypeContext
import org.stella.typecheck.StellaExtension
import org.stella.typecheck.StellaType
import org.stella.typecheck.TypeValidationException
import org.syntax.stella.Absyn.ABinding
import org.syntax.stella.Absyn.AMatchCase
import org.syntax.stella.Absyn.AParamDecl
import org.syntax.stella.Absyn.APatternBinding
import org.syntax.stella.Absyn.Abstraction
import org.syntax.stella.Absyn.Add
import org.syntax.stella.Absyn.Application
import org.syntax.stella.Absyn.Assign
import org.syntax.stella.Absyn.ConsList
import org.syntax.stella.Absyn.ConstFalse
import org.syntax.stella.Absyn.ConstInt
import org.syntax.stella.Absyn.ConstMemory
import org.syntax.stella.Absyn.ConstTrue
import org.syntax.stella.Absyn.ConstUnit
import org.syntax.stella.Absyn.Deref
import org.syntax.stella.Absyn.Divide
import org.syntax.stella.Absyn.DotRecord
import org.syntax.stella.Absyn.DotTuple
import org.syntax.stella.Absyn.Equal
import org.syntax.stella.Absyn.Expr
import org.syntax.stella.Absyn.Fix
import org.syntax.stella.Absyn.Fold
import org.syntax.stella.Absyn.GreaterThan
import org.syntax.stella.Absyn.GreaterThanOrEqual
import org.syntax.stella.Absyn.Head
import org.syntax.stella.Absyn.If
import org.syntax.stella.Absyn.Inl
import org.syntax.stella.Absyn.Inr
import org.syntax.stella.Absyn.IsEmpty
import org.syntax.stella.Absyn.IsZero
import org.syntax.stella.Absyn.LessThan
import org.syntax.stella.Absyn.LessThanOrEqual
import org.syntax.stella.Absyn.Let
import org.syntax.stella.Absyn.LetRec
import org.syntax.stella.Absyn.List
import org.syntax.stella.Absyn.LogicAnd
import org.syntax.stella.Absyn.LogicNot
import org.syntax.stella.Absyn.LogicOr
import org.syntax.stella.Absyn.Match
import org.syntax.stella.Absyn.MatchCase
import org.syntax.stella.Absyn.Multiply
import org.syntax.stella.Absyn.NatRec
import org.syntax.stella.Absyn.NoPatternData
import org.syntax.stella.Absyn.NotEqual
import org.syntax.stella.Absyn.Panic
import org.syntax.stella.Absyn.Pattern
import org.syntax.stella.Absyn.PatternFalse
import org.syntax.stella.Absyn.PatternInl
import org.syntax.stella.Absyn.PatternInr
import org.syntax.stella.Absyn.PatternTrue
import org.syntax.stella.Absyn.PatternUnit
import org.syntax.stella.Absyn.PatternVar
import org.syntax.stella.Absyn.PatternVariant
import org.syntax.stella.Absyn.Pred
import org.syntax.stella.Absyn.Record
import org.syntax.stella.Absyn.Ref
import org.syntax.stella.Absyn.Sequence
import org.syntax.stella.Absyn.SomeExprData
import org.syntax.stella.Absyn.SomePatternData
import org.syntax.stella.Absyn.Subtract
import org.syntax.stella.Absyn.Succ
import org.syntax.stella.Absyn.Tail
import org.syntax.stella.Absyn.Throw
import org.syntax.stella.Absyn.TryCatch
import org.syntax.stella.Absyn.TryWith
import org.syntax.stella.Absyn.Tuple
import org.syntax.stella.Absyn.TypeAbstraction
import org.syntax.stella.Absyn.TypeApplication
import org.syntax.stella.Absyn.TypeAsc
import org.syntax.stella.Absyn.TypeCast
import org.syntax.stella.Absyn.Unfold
import org.syntax.stella.Absyn.Var
import org.syntax.stella.Absyn.Variant
import kotlin.collections.List as ListKt

class ExprVisitor : Expr.Visitor<StellaType, FunctionTypeContext> {

    private fun checkExpectedType(expr: Expr, expected: StellaType, actual: StellaType) {
        if (expected != actual) {
            TypeValidationException.errorUnexpectedTypeForExpression(expr, expected, actual)
        }
    }

    override fun visit(
        p: If,
        arg: FunctionTypeContext
    ): StellaType? {
        val condType = p.expr_1.niceVisit(arg, null)
        checkExpectedType(p.expr_1, StellaType.Bool, condType)
        val leftType = p.expr_2.niceVisit(arg, null)
        val rightType = p.expr_3.niceVisit(arg, leftType)

        checkExpectedType(p.expr_3, leftType, rightType)

        return rightType
    }

    private fun checkNatOp(
        leftExpr: Expr,
        rightExpr: Expr,
        arg: FunctionTypeContext
    ): StellaType {
        val leftType = leftExpr.niceVisit(arg, StellaType.Nat)
        checkExpectedType(leftExpr, leftType, StellaType.Nat)

        val rightType = rightExpr.niceVisit(arg, StellaType.Nat)
        checkExpectedType(rightExpr, rightType, StellaType.Nat)

        return StellaType.Nat
    }

    override fun visit(
        p: LessThan,
        arg: FunctionTypeContext
    ): StellaType = checkNatOp(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: LessThanOrEqual,
        arg: FunctionTypeContext
    ): StellaType = checkNatOp(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: GreaterThan,
        arg: FunctionTypeContext
    ): StellaType = checkNatOp(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: GreaterThanOrEqual,
        arg: FunctionTypeContext
    ): StellaType = checkNatOp(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: TypeAsc,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.TYPE_ASCRIPTIONS)
        val asc = StellaType.fromAst(p.type_)

        return p.expr_.niceVisit(arg, asc)
    }

    override fun visit(
        p: Abstraction,
        arg: FunctionTypeContext
    ): StellaType? {
        val myArgs = p.listparamdecl_.map {
            it as AParamDecl
            it.stellaident_ to StellaType.fromAst(it.type_)
        }
        val myArgTypes = myArgs.map { it.second }
        val rt = if (arg.expectedType is StellaType.Fun) {
            if (myArgTypes != arg.expectedType.args) TypeValidationException.errorUnexpectedTypeForParameter()
            arg.expectedType.returnType
        } else null

        val t = p.expr_.niceVisit(arg.enterFunction(myArgs.toMap(), rt), rt)

        return StellaType.Fun(myArgTypes, t)
    }

    override fun visit(
        p: Variant,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.VARIANTS)
        if (arg.expectedType !is StellaType.Variant) TypeValidationException.errorAmbiguousVariantType()
        val hint = arg.expectedType.members.find { it.first == p.stellaident_ }
        if (hint == null) TypeValidationException.errorUnexpectedVariantLabel()

        val expr = if (p.exprdata_ is SomeExprData) p.exprdata_.expr_ else null
        if ((hint.second == null) != (expr == null)) {
            TypeValidationException.errorUnexpectedVariant()
        }

        if (expr != null) {
            val t = expr.niceVisit(arg, hint.second)
            checkExpectedType(expr, hint.second!!, t)
        }

        return arg.expectedType
    }

    private fun FunctionTypeContext.checkPattern(inputType: StellaType, pattern: Pattern): Map<String, StellaType> {
        this.checkExtension(StellaExtension.LET_BINDINGS)
        return when {
            pattern is PatternVar -> mapOf(pattern.stellaident_ to inputType)
            inputType is StellaType.Variant? && pattern is PatternVariant -> {
                this.checkExtension(StellaExtension.VARIANTS)
                val vt = inputType.members.find { (a, _) -> a == pattern.stellaident_ }
                    ?: TypeValidationException.errorUnexpectedPatternForType()
                when (pattern.patterndata_) {
                    is NoPatternData -> emptyMap()
                    is SomePatternData -> {
                        if (vt.second == null) TypeValidationException.errorUnexpectedPatternForType()
                        checkPattern(vt.second!!, pattern.patterndata_.pattern_)
                    }

                    else -> error("WTF")
                }
            }

            inputType is StellaType.Sum? && pattern is PatternInl -> {
                this.checkExtension(StellaExtension.SUM_TYPES)
                checkPattern(inputType.a, pattern.pattern_)
            }

            inputType is StellaType.Sum? && pattern is PatternInr -> {
                this.checkExtension(StellaExtension.SUM_TYPES)
                checkPattern(inputType.b, pattern.pattern_)
            }

            inputType is StellaType.Unit? && pattern is PatternUnit -> {
                this.checkExtension(StellaExtension.UNIT_TYPE)
                emptyMap()
            }

            inputType is StellaType.Bool? && (pattern is PatternFalse || pattern is PatternTrue) -> {
                emptyMap()
            }

            else -> TypeValidationException.errorUnexpectedPatternForType()
        }
    }

    override fun visit(
        p: Let,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.LET_BINDINGS)
        val newVars = p.listpatternbinding_
            .asSequence()
            .map { binding ->
                binding as APatternBinding
                val t = binding.expr_.niceVisit(arg, null)
                arg.checkPattern(t, binding.pattern_)
            }
            .fold(emptyMap<String, StellaType>()) { acc, map -> acc + map }

        return p.expr_.niceVisit(arg.withVariables(newVars), arg.expectedType)
    }

    private fun checkMatch(
        inputType: StellaType,
        cases: ListKt<MatchCase>,
        arg: FunctionTypeContext
    ): StellaType {
        if (cases.isEmpty()) TypeValidationException.errorIllegalEmptyMatching()

        var lastType: StellaType? = null
        for (case in cases) {
            val newVariables = arg.checkPattern(inputType, (case as AMatchCase).pattern_)
            val t = case.expr_.niceVisit(arg.withVariables(newVariables), arg.expectedType)
            if (lastType == null) {
                lastType = t
            } else {
                checkExpectedType(case.expr_, lastType, t)
            }
        }

        return lastType!!

    }

    override fun visit(
        p: Match,
        arg: FunctionTypeContext
    ): StellaType {
        arg.checkExtension(StellaExtension.SUM_TYPES)
        val inputType = p.expr_.niceVisit(arg, null)

        return checkMatch(inputType, p.listmatchcase_, arg)
    }

    override fun visit(
        p: List,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.LISTS)
        val expectedType =
            if (arg.expectedType is StellaType.List) arg.expectedType.type else null
        if (p.listexpr_.isEmpty()) {
            if (expectedType != null) {
                return StellaType.List(expectedType)
            } else {
                TypeValidationException.errorAmbiguousList()
            }
        }
        var lastType: StellaType? = null

        for (expr in p.listexpr_) {
            val t = expr.niceVisit(arg, expectedType)
            if (lastType == null) {
                lastType = t
            } else {
                checkExpectedType(expr, lastType, t)
            }
        }

        return StellaType.List(lastType!!)
    }

    override fun visit(
        p: Add,
        arg: FunctionTypeContext
    ): StellaType = checkNatOp(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: Subtract,
        arg: FunctionTypeContext
    ): StellaType = checkNatOp(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: Multiply,
        arg: FunctionTypeContext
    ): StellaType = checkNatOp(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: Divide,
        arg: FunctionTypeContext
    ): StellaType = checkNatOp(p.expr_1, p.expr_2, arg)

    private fun checkBoolOp(
        leftExpr: Expr,
        rightExpr: Expr,
        arg: FunctionTypeContext
    ): StellaType {
        val leftType = leftExpr.niceVisit(arg, StellaType.Bool)
        checkExpectedType(leftExpr, leftType, StellaType.Bool)

        val rightType = rightExpr.niceVisit(arg, StellaType.Bool)
        checkExpectedType(rightExpr, rightType, StellaType.Bool)

        return StellaType.Bool
    }

    override fun visit(
        p: LogicOr,
        arg: FunctionTypeContext
    ): StellaType = checkBoolOp(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: LogicAnd,
        arg: FunctionTypeContext
    ): StellaType = checkBoolOp(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: Application,
        arg: FunctionTypeContext
    ): StellaType? {
        val t = p.expr_.niceVisit(arg, null)
        if (t !is StellaType.Fun) {
            TypeValidationException.errorNotAFunction()
        }

        if (t.args.size != p.listexpr_.size) error("wrong number of arguments")


        t.args.zip(p.listexpr_).forEach { (a, b) ->
            val tt = b.niceVisit(arg, a)
            checkExpectedType(b, a, tt)
        }

        return t.returnType
    }

    override fun visit(
        p: DotRecord,
        arg: FunctionTypeContext
    ): StellaType? {
        val t = p.expr_.niceVisit(arg, null)
        if (t !is StellaType.Record) {
            TypeValidationException.errorNotARecord()
        }

        return t.members.find { it.first != p.stellaident_ }?.second
            ?: TypeValidationException.errorUnexpectedFieldAccess()
    }

    override fun visit(
        p: DotTuple,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.PAIRS)
        arg.checkExtension(StellaExtension.TUPLES)
        val t = p.expr_.niceVisit(arg, null)
        if (t !is StellaType.Tuple) {
            TypeValidationException.errorNotATuple()
        }


        return t.members.getOrNull(p.offset)
            ?: TypeValidationException.errorTupleIndexOutOfBounds()
    }

    override fun visit(
        p: Tuple,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.PAIRS)
        arg.checkExtension(StellaExtension.TUPLES)
        if (arg.expectedType is StellaType.Tuple && arg.expectedType.members.size != p.listexpr_.size) {
            TypeValidationException.errorUnexpectedTupleLength()
        }

        val expectedTypes = if (arg.expectedType is StellaType.Tuple) arg.expectedType.members else null

        val types = p.listexpr_.mapIndexed { i, it -> it.niceVisit(arg, expectedTypes?.getOrNull(i)) }

        return StellaType.Tuple(types)
    }

    override fun visit(
        p: Record,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.RECORDS)
        val expectedTypes = if (arg.expectedType is StellaType.Record) arg.expectedType.members.toMap() else null

        if (expectedTypes != null) {
            val expectedNames = expectedTypes.keys
            val actualNames = p.listbinding_.map { (it as ABinding).stellaident_ }.toSet()

            if (actualNames.any { it !in expectedNames }) TypeValidationException.errorUnexpectedRecordFields()
            if (expectedNames.any { it !in actualNames }) TypeValidationException.errorMissingRecordFields()
        }

        val types = p.listbinding_.map { it ->
            it as ABinding
            it.stellaident_ to it.expr_.niceVisit(arg, expectedTypes?.get(it.stellaident_))
        }

        return StellaType.Record(types)
    }

    override fun visit(
        p: ConsList,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.LISTS)
        val listUnderlyingType = (arg.expectedType as? StellaType.List)?.type
        val left = p.expr_1.niceVisit(arg, listUnderlyingType)
        val expectedRight = StellaType.List(left)
        val right = p.expr_2.niceVisit(arg, expectedRight)

        if (right != expectedRight) TypeValidationException.errorUnexpectedTypeForExpression(
            p.expr_2,
            expectedRight,
            right
        )

        return right
    }

    override fun visit(
        p: IsEmpty,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.LISTS)
        val t = p.niceVisit(arg, arg.expectedType)
        if (t !is StellaType.List) TypeValidationException.errorNotAList()

        return StellaType.Bool
    }

    private fun checkListUnaryOp(
        expr: Expr,
        arg: FunctionTypeContext,
    ): StellaType {
        val t = expr.niceVisit(arg, arg.expectedType)
        if (t !is StellaType.List) TypeValidationException.errorNotAList()

        return t.type
    }

    override fun visit(
        p: Head,
        arg: FunctionTypeContext
    ): StellaType {
        arg.checkExtension(StellaExtension.LISTS)
        return checkListUnaryOp(p.expr_, arg)
    }

    override fun visit(
        p: Tail,
        arg: FunctionTypeContext
    ): StellaType {
        arg.checkExtension(StellaExtension.LISTS)
        return checkListUnaryOp(p.expr_, arg)
    }

    private fun checkInj(
        expr: Expr,
        arg: FunctionTypeContext,
        block: (StellaType.Sum) -> StellaType
    ): StellaType.Sum {
        if (arg.expectedType == null) {
            TypeValidationException.errorAmbiguousSumType()
        }
        if (arg.expectedType !is StellaType.Sum) {
            TypeValidationException.errorUnexpectedInjection()
        }
        val expectedType = block(arg.expectedType)
        val t = expr.niceVisit(arg, expectedType)
        checkExpectedType(expr, expectedType, t)
        return arg.expectedType
    }

    override fun visit(
        p: Inl,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.SUM_TYPES)
        return checkInj(p.expr_, arg) { it.a }
    }

    override fun visit(
        p: Inr,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.SUM_TYPES)
        return checkInj(p.expr_, arg) { it.b }
    }

    override fun visit(
        p: LogicNot,
        arg: FunctionTypeContext
    ): StellaType? {
        checkExpectedType(p.expr_, StellaType.Bool, p.expr_.niceVisit(arg, StellaType.Bool))
        return StellaType.Bool
    }

    private fun checkNatUnaryOp(
        expr: Expr,
        arg: FunctionTypeContext,
        onSuccess: StellaType
    ): StellaType {
        checkExpectedType(expr, StellaType.Nat, expr.niceVisit(arg, StellaType.Nat))

        return onSuccess
    }

    override fun visit(
        p: Succ,
        arg: FunctionTypeContext
    ): StellaType = checkNatUnaryOp(p.expr_, arg, StellaType.Nat)

    override fun visit(
        p: Pred,
        arg: FunctionTypeContext
    ): StellaType = checkNatUnaryOp(p.expr_, arg, StellaType.Nat)

    override fun visit(
        p: IsZero,
        arg: FunctionTypeContext
    ): StellaType = checkNatUnaryOp(p.expr_, arg, StellaType.Bool)

    override fun visit(
        p: Fix?,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.FIX_POINT_COMBINATOR)
        TODO("Not yet implemented")
    }

    override fun visit(
        p: NatRec,
        arg: FunctionTypeContext
    ): StellaType? {
        val ct = p.expr_1.niceVisit(arg, StellaType.Nat)
        checkExpectedType(p.expr_1, StellaType.Nat, ct)

        val ft = p.expr_2.niceVisit(arg, arg.expectedType)

        val functionType = StellaType.Fun(listOf(StellaType.Nat), StellaType.Fun(listOf(ft), ft))
        val st = p.expr_3.niceVisit(arg, functionType)

        checkExpectedType(p.expr_3, functionType, st)

        return ft
    }

    override fun visit(
        p: ConstTrue?,
        arg: FunctionTypeContext?
    ): StellaType = StellaType.Bool

    override fun visit(
        p: ConstFalse?,
        arg: FunctionTypeContext?
    ): StellaType = StellaType.Bool

    override fun visit(
        p: ConstUnit?,
        arg: FunctionTypeContext
    ): StellaType {
        arg.checkExtension(StellaExtension.UNIT_TYPE)
        return StellaType.Unit
    }

    override fun visit(
        p: ConstInt?,
        arg: FunctionTypeContext?
    ): StellaType = StellaType.Nat

    override fun visit(
        p: Var,
        arg: FunctionTypeContext
    ): StellaType {
        return arg.variables[p.stellaident_] ?: TypeValidationException.errorUndefinedVariable()
    }

    // ---------------------------------
    override fun visit(
        p: ConstMemory?,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.REFERENCES)
        arg.expectedType ?: TypeValidationException.errorAmbiguousReferenceType()
        arg.expectedType as? StellaType.Reference ?: TypeValidationException.errorAmbiguousReferenceType()

        return arg.expectedType
    }

    override fun visit(
        p: Sequence,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.SEQUENCE)
        p.expr_1.niceVisit(arg, StellaType.Unit)
        return p.expr_2.niceVisit(arg, arg.expectedType)
    }

    override fun visit(
        p: Assign,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.REFERENCES)
        val right = p.expr_2.niceVisit(arg, null)
        val left = p.expr_1.niceVisit(arg, StellaType.Reference(right))

        if (left !is StellaType.Reference) TypeValidationException.errorNotAReference()
        if (left.underlyingType != right) TypeValidationException.errorUnexpectedSubtype()

        return StellaType.Unit
    }

    override fun visit(
        p: LetRec?,
        arg: FunctionTypeContext?
    ): StellaType? {
        TODO("Not yet implemented")
    }

    override fun visit(
        p: TypeAbstraction?,
        arg: FunctionTypeContext?
    ): StellaType? {
        TODO("Not yet implemented")
    }

    override fun visit(
        p: Equal?,
        arg: FunctionTypeContext?
    ): StellaType? {
        TODO("Not yet implemented")
    }

    override fun visit(
        p: NotEqual?,
        arg: FunctionTypeContext?
    ): StellaType? {
        TODO("Not yet implemented")
    }

    override fun visit(
        p: TypeCast?,
        arg: FunctionTypeContext?
    ): StellaType? {
        TODO("Not yet implemented")
    }

    override fun visit(
        p: Ref,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.REFERENCES)
        val expected = (arg.expectedType as? StellaType.Reference)?.underlyingType
        return StellaType.Reference(p.expr_.niceVisit(arg, expected))
    }

    override fun visit(
        p: Deref,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.REFERENCES)
        val expected = arg.expectedType?.let { StellaType.Reference(it) }
        val calculated = p.expr_.niceVisit(arg, expected)
        if (calculated !is StellaType.Reference) TypeValidationException.errorNotAReference()
        return calculated.underlyingType
    }

    override fun visit(
        p: Panic,
        arg: FunctionTypeContext
    ): StellaType {
        arg.checkExtension(StellaExtension.PANIC)
        return arg.expectedType ?: TypeValidationException.errorAmbiguousPanicType()
    }

    override fun visit(
        p: Throw,
        arg: FunctionTypeContext
    ): StellaType {
        arg.checkExtension(StellaExtension.EXCEPTIONS)
        if (arg.exceptionType == null) TypeValidationException.errorExceptionTypeNotDeclared()
        val t = p.expr_.niceVisit(arg, arg.exceptionType)
        if (t != arg.exceptionType) TypeValidationException.errorUnexpectedTypeForExpression(
            p.expr_,
            arg.exceptionType,
            t
        )

        return arg.expectedType ?: TypeValidationException.errorAmbiguousThrowType()
    }

    override fun visit(
        p: TryCatch,
        arg: FunctionTypeContext
    ): StellaType {
        arg.checkExtension(StellaExtension.EXCEPTIONS)
        arg.checkExtension(StellaExtension.EXCEPTIONS_TYPE_ANNOTATION)
        if (arg.exceptionType == null) TypeValidationException.errorExceptionTypeNotDeclared()
        val variables = arg.checkPattern(arg.exceptionType, p.pattern_)
        val f = p.expr_1.niceVisit(arg, arg.expectedType)
        val s = p.expr_2.niceVisit(arg.withVariables(variables), arg.expectedType)

        if (f != s) TypeValidationException.errorUnexpectedTypeForExpression(p.expr_2, f, s)

        return f
    }

    override fun visit(
        p: TryWith,
        arg: FunctionTypeContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.EXCEPTIONS)
        val f = p.expr_1.niceVisit(arg, arg.expectedType)
        val s = p.expr_2.niceVisit(arg, arg.expectedType)

        if (f != s) TypeValidationException.errorUnexpectedTypeForExpression(p.expr_2, f, s)

        return f
    }

    override fun visit(
        p: Fold?,
        arg: FunctionTypeContext?
    ): StellaType? {
        TODO("Not yet implemented")
    }

    override fun visit(
        p: Unfold?,
        arg: FunctionTypeContext?
    ): StellaType? {
        TODO("Not yet implemented")
    }

    override fun visit(
        p: TypeApplication?,
        arg: FunctionTypeContext?
    ): StellaType? {
        TODO("Not yet implemented")
    }


    private fun Expr.niceVisit(arg: FunctionTypeContext, expectedType: StellaType?): StellaType =
        this.accept(this@ExprVisitor, arg.withExpectedType(expectedType))

    private fun FunctionTypeContext.checkExtension(extension: StellaExtension) {
        if ("#" + extension.extensionName !in this.extensions)
            TypeValidationException.errorExtensionIsDisabled(extension.extensionName)
    }
}