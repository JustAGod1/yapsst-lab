package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaType
import org.stella.typecheck.TypeValidationException
import org.syntax.stella.Absyn.ALabelledPattern
import org.syntax.stella.Absyn.PatternAsc
import org.syntax.stella.Absyn.PatternCastAs
import org.syntax.stella.Absyn.PatternCons
import org.syntax.stella.Absyn.PatternFalse
import org.syntax.stella.Absyn.PatternInl
import org.syntax.stella.Absyn.PatternInr
import org.syntax.stella.Absyn.PatternInt
import org.syntax.stella.Absyn.PatternList
import org.syntax.stella.Absyn.PatternRecord
import org.syntax.stella.Absyn.PatternSucc
import org.syntax.stella.Absyn.PatternTrue
import org.syntax.stella.Absyn.PatternTuple
import org.syntax.stella.Absyn.PatternUnit
import org.syntax.stella.Absyn.PatternVar
import org.syntax.stella.Absyn.PatternVariant
import org.syntax.stella.Absyn.SomePatternData
import org.syntax.stella.Absyn.Pattern as StellaPattern

abstract class PatternModel {

    abstract fun checkConforms(context: FunctionContext, type: StellaType)

    abstract fun createBindings(context: FunctionContext, type: StellaType): List<Pair<String, StellaType>>

    open fun createBindingsLetrec(context: FunctionContext): List<Pair<String, StellaType>> {
        TypeValidationException.errorAmbiguousPatternType()
    }

    open fun unwrap() = this


    companion object {

        fun fromAst(pat: StellaPattern, context: FunctionContext): PatternModel {
            return when (pat) {
                is PatternList -> ListPattern(pat.listpattern_.map { fromAst(it, context) }, null)
                is PatternCons -> ListPattern(listOf(fromAst(pat.pattern_1, context)), fromAst(pat.pattern_2, context))

                is PatternRecord -> RecordPattern(pat.listlabelledpattern_.map {
                    it as ALabelledPattern
                    it.stellaident_ to fromAst(it.pattern_, context)
                })

                is PatternSucc -> SuccPattern(fromAst(pat.pattern_, context))

                is PatternAsc -> AscriptionPattern(
                    fromAst(pat.pattern_, context),
                    StellaType.fromAst(pat.type_, context)
                )

                is PatternCastAs -> CastPattern(fromAst(pat.pattern_, context), StellaType.fromAst(pat.type_, context))

                is PatternUnit -> UnitPattern

                is PatternInl -> SumPattern(fromAst(pat.pattern_, context), true)
                is PatternInr -> SumPattern(fromAst(pat.pattern_, context), false)

                is PatternFalse -> BoolPattern(false)
                is PatternTrue -> BoolPattern(true)

                is PatternInt -> NatPattern(pat.integer_)

                is PatternVar -> BindingPattern(pat.stellaident_)

                is PatternVariant -> VariantPattern(
                    pat.stellaident_,
                    (pat.patterndata_ as? SomePatternData)?.let { fromAst(it.pattern_, context) }
                )

                is PatternTuple -> TuplePattern(pat.listpattern_.map { fromAst(it, context) })

                else -> throw IllegalArgumentException("Unexpected pattern type ${pat.javaClass}")
            }
        }

    }
}