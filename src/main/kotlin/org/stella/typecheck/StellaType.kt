package org.stella.typecheck


import org.syntax.stella.Absyn.ARecordFieldType
import org.syntax.stella.Absyn.AVariantFieldType
import org.syntax.stella.Absyn.SomeTyping
import org.syntax.stella.Absyn.Type
import org.syntax.stella.Absyn.TypeBool
import org.syntax.stella.Absyn.TypeFun
import org.syntax.stella.Absyn.TypeList
import org.syntax.stella.Absyn.TypeNat
import org.syntax.stella.Absyn.TypeRecord
import org.syntax.stella.Absyn.TypeSum
import org.syntax.stella.Absyn.TypeTuple
import org.syntax.stella.Absyn.TypeUnit
import org.syntax.stella.Absyn.TypeVariant
import kotlin.collections.List as KtList

sealed class StellaType(private val desc: String) {
    override fun toString(): String {
        return desc
    }

    object Nat : StellaType("Nat")
    object Unit : StellaType("Unit")
    object Bool : StellaType("Bool")

    data class Record(val members: KtList<Pair<String, StellaType>>) : StellaType("{" + members.joinToString { (a, b) -> "$a: $b" } + "}")
    data class Tuple(val members: KtList<StellaType>) : StellaType("{" + members.joinToString() + "}")
    data class List(val type: StellaType) : StellaType("[$type]")
    data class Sum(val a: StellaType, val b: StellaType) : StellaType("$a + $b")
    data class Variant(val members: KtList<Pair<String, StellaType?>>) : StellaType("<|" + members.joinToString { (a, b) -> "$a: $b" } + "|>")
    data class Fun(val args: KtList<StellaType>, val returnType: StellaType) : StellaType("(" + args.joinToString() + ") -> " + returnType)

    companion object {
        fun fromAst(t: Type) : StellaType {
            return when (t) {
                is TypeNat -> Nat
                is TypeBool -> Bool
                is TypeUnit -> Unit

                is TypeRecord -> Record(t.listrecordfieldtype_.map {
                    it as ARecordFieldType
                    it.stellaident_ to fromAst(it.type_)
                })

                is TypeTuple -> Tuple(t.listtype_.map(::fromAst))

                is TypeList -> List(fromAst(t.type_))

                is TypeSum -> Sum(fromAst(t.type_1), fromAst(t.type_2))

                is TypeVariant -> Variant(t.listvariantfieldtype_.map {
                    it as AVariantFieldType
                    it.stellaident_ to if (it.optionaltyping_ is SomeTyping) fromAst(it.optionaltyping_.type_) else null
                })

                is TypeFun -> Fun(t.listtype_.map(::fromAst), fromAst(t.type_))

                else -> error("Type ${t.javaClass.simpleName} is not supported")
            }
        }
    }
}


