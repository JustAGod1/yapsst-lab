package org.stella.typecheck


import org.syntax.stella.Absyn.ARecordFieldType
import org.syntax.stella.Absyn.AVariantFieldType
import org.syntax.stella.Absyn.SomeTyping
import org.syntax.stella.Absyn.Type
import org.syntax.stella.Absyn.TypeAuto
import org.syntax.stella.Absyn.TypeBool
import org.syntax.stella.Absyn.TypeBottom
import org.syntax.stella.Absyn.TypeFun
import org.syntax.stella.Absyn.TypeList
import org.syntax.stella.Absyn.TypeNat
import org.syntax.stella.Absyn.TypeRecord
import org.syntax.stella.Absyn.TypeRef
import org.syntax.stella.Absyn.TypeSum
import org.syntax.stella.Absyn.TypeTop
import org.syntax.stella.Absyn.TypeTuple
import org.syntax.stella.Absyn.TypeUnit
import org.syntax.stella.Absyn.TypeVariant
import kotlin.collections.List as KtList

sealed class StellaType(private val desc: String, val hasAuto: Boolean) {
    override fun toString(): String {
        return desc
    }

    open fun isAssignableFrom(that: StellaType): Boolean {
        return that == Bot || that == this
    }

    abstract fun substitute(context: ReconstructionContext): StellaType

    object Nat : StellaType("Nat", false) {
        override fun substitute(context: ReconstructionContext): StellaType = this
    }

    object Unit : StellaType("Unit", false) {
        override fun substitute(context: ReconstructionContext): StellaType = this
    }

    object Bool : StellaType("Bool", false) {
        override fun substitute(context: ReconstructionContext): StellaType = this
    }

    object Top : StellaType("Top", false) {
        override fun substitute(context: ReconstructionContext): StellaType = this

        override fun isAssignableFrom(that: StellaType) = true
    }

    object Bot : StellaType("Bot", false) {
        override fun substitute(context: ReconstructionContext): StellaType = this
    }

    data class Record(val members: Map<String, StellaType>) :
        StellaType("{" + members.entries.joinToString { (a, b) -> "$a: $b" } + "}", members.values.any { it.hasAuto }) {
        override fun substitute(context: ReconstructionContext): StellaType {
            return Record(
                members.entries.associate { (a, b) -> a to b.substitute(context) }
            )
        }

        override fun isAssignableFrom(that: StellaType): Boolean {
            if (super.isAssignableFrom(that)) return true

            if (that !is Record) return false

            for ((k, v) in this.members) {
                val thatV = that.members[k] ?: return false
                if (!v.isAssignableFrom(thatV)) return false
            }

            return true
        }
    }

    data class Tuple(val members: KtList<StellaType>) :
        StellaType("{" + members.joinToString() + "}", members.any { it.hasAuto }) {
        override fun substitute(context: ReconstructionContext): StellaType {
            return Tuple(members.map { it.substitute(context) })
        }

        override fun isAssignableFrom(that: StellaType): Boolean {
            if (super.isAssignableFrom(that)) return true

            if (that !is Tuple) return false

            for (t in members) {
                if (!that.members.any { t.isAssignableFrom(it) }) return false
            }
            return true
        }
    }

    data class List(val type: StellaType) : StellaType("[$type]", type.hasAuto) {
        override fun substitute(context: ReconstructionContext): StellaType {
            return List(type.substitute(context))
        }

        override fun isAssignableFrom(that: StellaType): Boolean {
            if (super.isAssignableFrom(that)) return true
            if (that !is List) return false
            return type.isAssignableFrom(that.type)
        }
    }

    data class Reference(val underlyingType: StellaType) : StellaType("*$underlyingType", underlyingType.hasAuto) {
        override fun substitute(context: ReconstructionContext): StellaType {
            return Reference(underlyingType.substitute(context))
        }

        override fun isAssignableFrom(that: StellaType): Boolean {
            if (super.isAssignableFrom(that)) return true

            if (that !is Reference) return false

            return underlyingType.isAssignableFrom(that.underlyingType)
        }
    }

    data class Sum(val a: StellaType, val b: StellaType) : StellaType("$a + $b", a.hasAuto || b.hasAuto) {
        override fun substitute(context: ReconstructionContext): StellaType {
            return Sum(a.substitute(context), b.substitute(context))
        }

        override fun isAssignableFrom(that: StellaType): Boolean {
            if (super.isAssignableFrom(that)) return true

            if (that !is Sum) return false

            if (!a.isAssignableFrom(that.a)) return false
            if (!b.isAssignableFrom(that.b)) return false

            return true
        }
    }

    data class Variant(val members: KtList<Pair<String, StellaType?>>) :
        StellaType("<|" + members.joinToString { (a, b) -> "$a: $b" } + "|>",
            members.any { it.second?.hasAuto == true }) {
        override fun substitute(context: ReconstructionContext): StellaType {
            return Variant(
                members.map { it.first to it.second?.substitute(context) },
            )
        }
    }

    data class Fun(val args: KtList<StellaType>, val returnType: StellaType) :
        StellaType("(" + args.joinToString() + ") -> " + returnType, args.any { it.hasAuto } || returnType.hasAuto) {
        override fun substitute(context: ReconstructionContext): StellaType {
            return Fun(args.map { it.substitute(context) }, returnType.substitute(context))
        }
    }

    data class Auto(val idx: Int) : StellaType("t$idx", true) {
        override fun substitute(context: ReconstructionContext): StellaType {
            return context.substitute(this)
        }
    }

    companion object {
        fun fromAst(t: Type, context: FunctionContext): StellaType {
            return when (t) {
                is TypeNat -> Nat
                is TypeBool -> Bool
                is TypeUnit -> Unit

                is TypeRecord -> Record(t.listrecordfieldtype_.associate {
                    it as ARecordFieldType
                    it.stellaident_ to fromAst(it.type_, context)
                })

                is TypeTuple -> Tuple(t.listtype_.map { fromAst(it, context) })

                is TypeList -> List(fromAst(t.type_, context))

                is TypeSum -> Sum(fromAst(t.type_1, context), fromAst(t.type_2, context))

                is TypeVariant -> Variant(t.listvariantfieldtype_.map {
                    it as AVariantFieldType
                    it.stellaident_ to if (it.optionaltyping_ is SomeTyping) fromAst(
                        it.optionaltyping_.type_,
                        context
                    ) else null
                })

                is TypeFun -> Fun(t.listtype_.map { fromAst(it, context) }, fromAst(t.type_, context))
                is TypeRef -> Reference(fromAst(t.type_, context))
                is TypeAuto -> {
                    context.checkExtension(StellaExtension.TYPE_RECONSTRUCTION)
                    context.persistent.reconstruction.autoMap(t)
                }

                is TypeTop -> {
                    context.checkExtension(StellaExtension.TOP_TYPE)
                    Top
                }

                is TypeBottom -> {
                    context.checkExtension(StellaExtension.BOTTOM_TYPE)
                    Bot
                }

                else -> error("Type ${t.javaClass.simpleName} is not supported")
            }
        }
    }
}



