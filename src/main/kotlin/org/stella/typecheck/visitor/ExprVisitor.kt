package org.stella.typecheck.visitor

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaExtension
import org.stella.typecheck.StellaType
import org.stella.typecheck.TypeValidationException
import org.stella.typecheck.matching.MatchingSlice
import org.stella.typecheck.matching.PatternModel
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
import org.syntax.stella.Absyn.NotEqual
import org.syntax.stella.Absyn.Panic
import org.syntax.stella.Absyn.Pattern
import org.syntax.stella.Absyn.Pred
import org.syntax.stella.Absyn.Record
import org.syntax.stella.Absyn.Ref
import org.syntax.stella.Absyn.Sequence
import org.syntax.stella.Absyn.SomeExprData
import org.syntax.stella.Absyn.Subtract
import org.syntax.stella.Absyn.Succ
import org.syntax.stella.Absyn.Tail
import org.syntax.stella.Absyn.Throw
import org.syntax.stella.Absyn.TryCastAs
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

class ExprVisitor : Expr.Visitor<StellaType, FunctionContext> {

    override fun visit(
        p: If,
        arg: FunctionContext
    ): StellaType? {
        val condType = p.expr_1.niceVisit(arg, StellaType.Bool)
        arg.cmpTypesOrAddConstraint(p.expr_1, StellaType.Bool, condType)
        val leftType = p.expr_2.niceVisit(arg, arg.expectedType)
        val rightType = p.expr_3.niceVisit(arg, leftType)

        arg.cmpTypesOrAddConstraint(p.expr_3, leftType, rightType)

        return rightType
    }

    private fun checkNatOpBool(
        leftExpr: Expr,
        rightExpr: Expr,
        arg: FunctionContext,
    ): StellaType = doCheckNatOp(leftExpr, rightExpr, arg, StellaType.Bool)

    private fun checkNatOpNat(
        leftExpr: Expr,
        rightExpr: Expr,
        arg: FunctionContext,
    ): StellaType = doCheckNatOp(leftExpr, rightExpr, arg, StellaType.Nat)

    private fun doCheckNatOp(
        leftExpr: Expr,
        rightExpr: Expr,
        arg: FunctionContext,
        retType: StellaType
    ): StellaType {
        val leftType = leftExpr.niceVisit(arg, StellaType.Nat)
        arg.cmpTypesOrAddConstraint(leftExpr, leftType, StellaType.Nat)

        val rightType = rightExpr.niceVisit(arg, StellaType.Nat)
        arg.cmpTypesOrAddConstraint(rightExpr, rightType, StellaType.Nat)

        return retType
    }

    override fun visit(
        p: LessThan,
        arg: FunctionContext
    ): StellaType = checkNatOpBool(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: LessThanOrEqual,
        arg: FunctionContext
    ): StellaType = checkNatOpBool(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: GreaterThan,
        arg: FunctionContext
    ): StellaType = checkNatOpBool(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: GreaterThanOrEqual,
        arg: FunctionContext
    ): StellaType = checkNatOpBool(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: TypeAsc,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.TYPE_ASCRIPTIONS)
        val asc = StellaType.fromAst(p.type_, arg)

        val t = p.expr_.niceVisit(arg, asc)
        arg.cmpTypesOrAddConstraint(p.expr_, asc, t)
        return asc
    }

    override fun visit(
        p: Abstraction,
        arg: FunctionContext
    ): StellaType? {
        val myArgs = p.listparamdecl_.map {
            it as AParamDecl
            it.stellaident_ to StellaType.fromAst(it.type_, arg)
        }
        val myArgTypes = myArgs.map { it.second }
        val rt = if (arg.expectedType is StellaType.Fun) {
            arg.expectedType.returnType
        } else null

        val t = p.expr_.niceVisit(arg.enterFunction(myArgs.toMap(), rt), rt)

        if (rt != null) {
            arg.cmpTypesOrAddConstraint(p.expr_, rt, t)
        }

        return StellaType.Fun(myArgTypes, t)
    }

    override fun visit(
        p: Variant,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.VARIANTS)
        if (arg.expectedType !is StellaType.Variant) TypeValidationException.errorAmbiguousVariantType()
        if (p.stellaident_ !in arg.expectedType.members) TypeValidationException.errorUnexpectedVariantLabel(p.stellaident_)
        val hint = arg.expectedType.members[p.stellaident_]

        val expr = if (p.exprdata_ is SomeExprData) p.exprdata_.expr_ else null
        if (hint != null && expr == null) {
            TypeValidationException.errorMissingDataForLabel(p.stellaident_)
        }
        if (hint == null && expr != null) {
            TypeValidationException.errorUnexpectedDataForNullaryLabel(p.stellaident_)
        }

        if (expr != null) {
            val t = expr.niceVisit(arg, hint)
            arg.cmpTypesOrAddConstraint(expr, hint!!, t)
        }

        return arg.expectedType
    }

    private fun FunctionContext.checkPattern(inputType: StellaType, pattern: Pattern): Map<String, StellaType> {
        val p = org.stella.typecheck.matching.PatternModel.fromAst(pattern, this)
        p.checkConforms(this, inputType)
        return p.createBindings(this, inputType).toMap()
    }

    override fun visit(
        p: Let,
        arg: FunctionContext
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
        arg: FunctionContext
    ): StellaType {
        if (cases.isEmpty()) TypeValidationException.errorIllegalEmptyMatching()

        var lastType: StellaType? = null
        for (case in cases) {
            val newVariables = arg.checkPattern(inputType, (case as AMatchCase).pattern_)
            val t = case.expr_.niceVisit(arg.withVariables(newVariables), lastType ?: arg.expectedType)
            if (lastType == null) {
                lastType = t
            } else {
                arg.cmpTypesOrAddConstraint(case.expr_, lastType, t)
            }
        }

        arg.persistent.postponedMatches += inputType to MatchingSlice(
            cases.map {
                it as AMatchCase
                PatternModel.fromAst(it.pattern_, arg)
            }
        )

        return lastType!!

    }

    override fun visit(
        p: Match,
        arg: FunctionContext
    ): StellaType {
        arg.checkExtension(StellaExtension.SUM_TYPES)
        val inputType = p.expr_.niceVisit(arg, null)

        return checkMatch(inputType, p.listmatchcase_, arg)
    }

    override fun visit(
        p: List,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.LISTS)
        val expectedType =
            if (arg.expectedType is StellaType.List) arg.expectedType.type else null
        if (p.listexpr_.isEmpty()) {
            if (expectedType != null) {
                return StellaType.List(expectedType)
            } else {
                if (arg.expectedType == null) TypeValidationException.errorAmbiguousList()
                else TypeValidationException.errorUnexpectedList()
            }
        }
        var lastType: StellaType? = null

        for (expr in p.listexpr_) {
            val t = expr.niceVisit(arg, expectedType)
            if (lastType == null) {
                lastType = t
            } else {
                arg.cmpTypesOrAddConstraint(expr, lastType, t)
            }
        }

        return StellaType.List(lastType!!)
    }

    override fun visit(
        p: Add,
        arg: FunctionContext
    ): StellaType = checkNatOpNat(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: Subtract,
        arg: FunctionContext
    ): StellaType = checkNatOpNat(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: Multiply,
        arg: FunctionContext
    ): StellaType = checkNatOpNat(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: Divide,
        arg: FunctionContext
    ): StellaType = checkNatOpNat(p.expr_1, p.expr_2, arg)

    private fun checkBoolOp(
        leftExpr: Expr,
        rightExpr: Expr,
        arg: FunctionContext
    ): StellaType {
        val leftType = leftExpr.niceVisit(arg, StellaType.Bool)
        arg.cmpTypesOrAddConstraint(leftExpr, leftType, StellaType.Bool)

        val rightType = rightExpr.niceVisit(arg, StellaType.Bool)
        arg.cmpTypesOrAddConstraint(rightExpr, rightType, StellaType.Bool)

        return StellaType.Bool
    }

    override fun visit(
        p: LogicOr,
        arg: FunctionContext
    ): StellaType = checkBoolOp(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: LogicAnd,
        arg: FunctionContext
    ): StellaType = checkBoolOp(p.expr_1, p.expr_2, arg)

    override fun visit(
        p: Application,
        arg: FunctionContext
    ): StellaType? {
        val exprType = p.expr_.niceVisit(arg, null)
        if (exprType !is StellaType.Fun && exprType !is StellaType.Auto) {
            TypeValidationException.errorNotAFunction()
        }
        val expectedArgTypes = (exprType as? StellaType.Fun)?.args
        val expectedReturnType = (exprType as? StellaType.Fun)?.returnType ?: arg.persistent.reconstruction.atom()
        val argTypes = p.listexpr_.mapIndexed { i, it ->
            it.niceVisit(arg, expectedArgTypes?.getOrNull(i))
        }

        arg.cmpTypesOrAddConstraint(p, StellaType.Fun(argTypes, expectedReturnType), exprType)

        return expectedReturnType
    }

    override fun visit(
        p: DotRecord,
        arg: FunctionContext
    ): StellaType? {
        val t = p.expr_.niceVisit(arg, null)
        if (t !is StellaType.Record) {
            TypeValidationException.errorNotARecord()
        }

        return t.members.entries.find { it.key == p.stellaident_ }?.value
            ?: TypeValidationException.errorUnexpectedFieldAccess()
    }

    override fun visit(
        p: DotTuple,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.TUPLES)
        val t = p.expr_.niceVisit(arg, null)
        if (t !is StellaType.Tuple) {
            TypeValidationException.errorNotATuple()
        }


        return t.members.getOrNull(p.integer_ - 1)
            ?: TypeValidationException.errorTupleIndexOutOfBounds()
    }

    override fun visit(
        p: Tuple,
        arg: FunctionContext
    ): StellaType? {
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
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.RECORDS)
        val expectedTypes = if (arg.expectedType is StellaType.Record) arg.expectedType.members.toMap() else null

        if (expectedTypes != null) {
            val expectedNames = expectedTypes.keys
            val actualNames = p.listbinding_.map { (it as ABinding).stellaident_ }.toSet()

            if (!arg.hasExtension(StellaExtension.STRUCTURAL_SUBTYPING) && actualNames.any { it !in expectedNames }) TypeValidationException.errorUnexpectedRecordFields()
            for (name in expectedNames) {
                if (name !in actualNames) TypeValidationException.errorMissingRecordFields(name)
            }
        }

        val types = p.listbinding_.map { it ->
            it as ABinding
            it.stellaident_ to it.expr_.niceVisit(arg, expectedTypes?.get(it.stellaident_))
        }

        return StellaType.Record(types)
    }

    override fun visit(
        p: ConsList,
        arg: FunctionContext
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
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.LISTS)
        val t = p.expr_.niceVisit(arg, arg.expectedType)
        if (t !is StellaType.List) TypeValidationException.errorNotAList()

        return StellaType.Bool
    }

    private fun checkListUnaryOp(
        expr: Expr,
        arg: FunctionContext,
    ): StellaType {
        val t = expr.niceVisit(arg, arg.expectedType)
        if (t !is StellaType.List) TypeValidationException.errorNotAList()

        return t.type
    }

    override fun visit(
        p: Head,
        arg: FunctionContext
    ): StellaType {
        arg.checkExtension(StellaExtension.LISTS)
        return checkListUnaryOp(p.expr_, arg)
    }

    override fun visit(
        p: Tail,
        arg: FunctionContext
    ): StellaType {
        arg.checkExtension(StellaExtension.LISTS)
        return StellaType.List(checkListUnaryOp(p.expr_, arg))
    }

    private fun checkInj(
        p: Expr,
        expr: Expr,
        arg: FunctionContext,
        left: Boolean
    ): StellaType.Sum {
        val (expectedOther, expectedMe) = if (arg.expectedType == null) {
            if (arg.hasExtension(StellaExtension.AMBIGUOUS_TYPE_AS_BOTTOM)) {
                StellaType.Bot to null
            } else if (arg.hasExtension(StellaExtension.TYPE_RECONSTRUCTION)) {
                arg.persistent.reconstruction.atom() to null
            } else {
                TypeValidationException.errorAmbiguousSumType()
            }
        } else if (arg.expectedType is StellaType.Sum) {
            if (left) arg.expectedType.b to arg.expectedType.a
            else arg.expectedType.a to arg.expectedType.b
        } else if (arg.expectedType.hasAuto) {
            arg.persistent.reconstruction.atom() to null
        } else {
            TypeValidationException.errorUnexpectedInjection()
        }

        val myType = expr.niceVisit(arg, expectedMe)

        return if (left) StellaType.Sum(myType, expectedOther)
        else StellaType.Sum(expectedOther, myType)
    }

    override fun visit(
        p: Inl,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.SUM_TYPES)
        return checkInj(p, p.expr_, arg, true)
    }

    override fun visit(
        p: Inr,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.SUM_TYPES)
        return checkInj(p, p.expr_, arg, false)
    }

    override fun visit(
        p: LogicNot,
        arg: FunctionContext
    ): StellaType? {
        arg.cmpTypesOrAddConstraint(p.expr_, StellaType.Bool, p.expr_.niceVisit(arg, StellaType.Bool))
        return StellaType.Bool
    }

    private fun checkNatUnaryOp(
        expr: Expr,
        arg: FunctionContext,
        onSuccess: StellaType
    ): StellaType {
        arg.cmpTypesOrAddConstraint(expr, StellaType.Nat, expr.niceVisit(arg, StellaType.Nat))

        return onSuccess
    }

    override fun visit(
        p: Succ,
        arg: FunctionContext
    ): StellaType = checkNatUnaryOp(p.expr_, arg, StellaType.Nat)

    override fun visit(
        p: Pred,
        arg: FunctionContext
    ): StellaType = checkNatUnaryOp(p.expr_, arg, StellaType.Nat)

    override fun visit(
        p: IsZero,
        arg: FunctionContext
    ): StellaType = checkNatUnaryOp(p.expr_, arg, StellaType.Bool)

    override fun visit(
        p: Fix,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.FIX_POINT_COMBINATOR)
        val expectedFunctionType = arg.expectedType?.let {StellaType.Fun(listOf(it), it) }
        val functionType = p.expr_.niceVisit(arg, expectedFunctionType)

        if (functionType !is StellaType.Auto && functionType !is StellaType.Fun) {
            TypeValidationException.errorNotAFunction()
        }

        val realFunctionType = if (functionType is StellaType.Auto) {
            val a = arg.persistent.reconstruction.atom()
            val t = StellaType.Fun(listOf(a), a)
            arg.persistent.reconstruction.applyConstraint(functionType, t)
            t
        } else {
            functionType as StellaType.Fun
        }

        if (realFunctionType.args.size != 1) {
            TypeValidationException.errorIncorrectNumberOfArguments()
        }

        arg.cmpTypesOrAddConstraint(null, realFunctionType.args[0], realFunctionType.returnType)

        return realFunctionType.returnType
    }

    override fun visit(
        p: NatRec,
        arg: FunctionContext
    ): StellaType? {
        val ct = p.expr_1.niceVisit(arg, StellaType.Nat)
        arg.cmpTypesOrAddConstraint(p.expr_1, StellaType.Nat, ct)

        val ft = p.expr_2.niceVisit(arg, arg.expectedType)

        val functionType = StellaType.Fun(listOf(StellaType.Nat), StellaType.Fun(listOf(ft), ft))
        val st = p.expr_3.niceVisit(arg, functionType)

        arg.cmpTypesOrAddConstraint(p.expr_3, functionType, st)

        return ft
    }

    override fun visit(
        p: ConstTrue?,
        arg: FunctionContext?
    ): StellaType = StellaType.Bool

    override fun visit(
        p: ConstFalse?,
        arg: FunctionContext?
    ): StellaType = StellaType.Bool

    override fun visit(
        p: ConstUnit?,
        arg: FunctionContext
    ): StellaType {
        arg.checkExtension(StellaExtension.UNIT_TYPE)
        return StellaType.Unit
    }

    override fun visit(
        p: ConstInt?,
        arg: FunctionContext?
    ): StellaType = StellaType.Nat

    override fun visit(
        p: Var,
        arg: FunctionContext
    ): StellaType {
        val v = arg.variables[p.stellaident_] ?: TypeValidationException.errorUndefinedVariable(p.stellaident_)
        return v.reshift(arg)
    }

    // ---------------------------------
    override fun visit(
        p: ConstMemory?,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.REFERENCES)
        arg.expectedType ?: TypeValidationException.errorAmbiguousReferenceType()
        arg.expectedType as? StellaType.Reference ?: TypeValidationException.errorAmbiguousReferenceType()

        return arg.expectedType
    }

    override fun visit(
        p: Sequence,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.SEQUENCE)
        val shouldBeUnit = p.expr_1.niceVisit(arg, StellaType.Unit)
        arg.cmpTypesOrAddConstraint(p.expr_1, StellaType.Unit, shouldBeUnit)
        return p.expr_2.niceVisit(arg, arg.expectedType)
    }

    override fun visit(
        p: Assign,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.REFERENCES)
        val right = p.expr_2.niceVisit(arg, null)
        val left = p.expr_1.niceVisit(arg, StellaType.Reference(right))

        if (left !is StellaType.Reference) TypeValidationException.errorNotAReference()
        arg.cmpTypesOrAddConstraint(p.expr_2, left.underlyingType, right)

        return StellaType.Unit
    }

    override fun visit(
        p: LetRec,
        arg: FunctionContext
    ): StellaType {
        val variables = hashMapOf<String, StellaType>()
        for (binding in p.listpatternbinding_) {
            binding as APatternBinding
            val currentContext = arg.withVariables(variables)
            val pat = PatternModel.fromAst(binding.pattern_, currentContext)
            variables += pat.createBindingsLetrec(currentContext)
            val calculatedType = binding.expr_.niceVisit(arg.withVariables(variables), null)
            arg.checkPattern(calculatedType, binding.pattern_)
        }

        return p.expr_.niceVisit(arg.withVariables(variables), arg.expectedType)
    }

    override fun visit(
        p: TypeAbstraction,
        arg: FunctionContext
    ): StellaType {
        val names = p.liststellaident_.toSet()
        val inner = p.expr_.niceVisit(arg.withTypeVariables(names, p), arg.expectedType)
        return StellaType.ForAll(names.toList(), inner)
    }

    override fun visit(
        p: Equal?,
        arg: FunctionContext?
    ): StellaType? {
        TODO("Not yet implemented")
    }

    override fun visit(
        p: NotEqual?,
        arg: FunctionContext?
    ): StellaType? {
        TODO("Not yet implemented")
    }

    override fun visit(
        p: TypeCast,
        arg: FunctionContext
    ): StellaType {
        arg.checkExtension(StellaExtension.TYPE_CAST)
        arg.checkExtension(StellaExtension.STRUCTURAL_SUBTYPING)

        val wideType = p.expr_.niceVisit(arg, null)
        val narrowType = StellaType.fromAst(p.type_, arg)

        //arg.cmpTypesOrAddConstraint(p.expr_, wideType, narrowType)

        return narrowType
    }

    override fun visit(
        p: Ref,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.REFERENCES)
        val expected = (arg.expectedType as? StellaType.Reference)?.underlyingType
        return StellaType.Reference(p.expr_.niceVisit(arg, expected))
    }

    override fun visit(
        p: Deref,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.REFERENCES)
        val expected = arg.expectedType?.let { StellaType.Reference(it) }
        val calculated = p.expr_.niceVisit(arg, expected)
        if (calculated !is StellaType.Reference) TypeValidationException.errorNotAReference()
        return calculated.underlyingType
    }

    override fun visit(
        p: Panic,
        arg: FunctionContext
    ): StellaType {
        arg.checkExtension(StellaExtension.PANIC)
        if (arg.expectedType != null) {
            return arg.expectedType
        } else if (arg.hasExtension(StellaExtension.AMBIGUOUS_TYPE_AS_BOTTOM)) {
            return StellaType.Bot
        } else {
            TypeValidationException.errorAmbiguousPanicType()
        }
    }

    override fun visit(
        p: Throw,
        arg: FunctionContext
    ): StellaType {
        arg.checkExtension(StellaExtension.EXCEPTIONS)
        if (arg.exceptionType == null) TypeValidationException.errorExceptionTypeNotDeclared()
        val t = p.expr_.niceVisit(arg, arg.exceptionType)
        arg.cmpTypesOrAddConstraint(
            p.expr_,
            arg.exceptionType!!,
            t
        )

        return arg.expectedType ?: TypeValidationException.errorAmbiguousThrowType()
    }

    override fun visit(
        p: TryCatch,
        arg: FunctionContext
    ): StellaType {
        arg.checkExtension(StellaExtension.EXCEPTIONS)
        arg.checkExtension(StellaExtension.EXCEPTIONS_TYPE_DECLARATION)
        if (arg.exceptionType == null) TypeValidationException.errorExceptionTypeNotDeclared()
        val variables = arg.checkPattern(arg.exceptionType!!, p.pattern_)
        val f = p.expr_1.niceVisit(arg, arg.expectedType)
        val s = p.expr_2.niceVisit(arg.withVariables(variables), arg.expectedType)

        if (f != s) TypeValidationException.errorUnexpectedTypeForExpression(p.expr_2, f, s)

        return f
    }

    override fun visit(
        p: TryWith,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.EXCEPTIONS)
        val f = p.expr_1.niceVisit(arg, arg.expectedType)
        val s = p.expr_2.niceVisit(arg, arg.expectedType)

        if (f != s) TypeValidationException.errorUnexpectedTypeForExpression(p.expr_2, f, s)

        return f
    }

    override fun visit(
        p: TryCastAs,
        arg: FunctionContext
    ): StellaType {
        val asc = StellaType.fromAst(p.type_, arg)
        p.expr_1.niceVisit(arg, asc)
        val pat = PatternModel.fromAst(p.pattern_, arg)
        pat.checkConforms(arg, asc)
        val bindings = pat.createBindings(arg, asc).toMap()

        val a = p.expr_2.niceVisit(arg.withVariables(bindings), arg.expectedType)
        val b = p.expr_3.niceVisit(arg, a)

        if (arg.expectedType != null) arg.cmpTypesOrAddConstraint(p.expr_2, arg.expectedType, a)
        arg.cmpTypesOrAddConstraint(p.expr_3, a, b)

        return b
    }

    override fun visit(
        p: Fold?,
        arg: FunctionContext?
    ): StellaType? {
        TODO("Not yet implemented")
    }

    override fun visit(
        p: Unfold?,
        arg: FunctionContext?
    ): StellaType? {
        TODO("Not yet implemented")
    }

    override fun visit(
        p: TypeApplication,
        arg: FunctionContext
    ): StellaType? {
        arg.checkExtension(StellaExtension.UNIVERSAL_TYPES)
        val inner = p.expr_.niceVisit(arg, arg.expectedType)
        if (inner !is StellaType.ForAll) {
            TypeValidationException.errorNotAGenericFunction()
        }

        val typeArgs = p.listtype_.map { StellaType.fromAst(it, arg) }
        val concretized = inner.reify(typeArgs)

        return concretized
    }


    private fun Expr.niceVisit(arg: FunctionContext, expectedType: StellaType?): StellaType {
        val context = arg.withExpectedType(expectedType)
        val result = this.accept(this@ExprVisitor, context)

        return result
    }

}