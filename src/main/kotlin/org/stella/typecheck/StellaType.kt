package org.stella.typecheck


import org.syntax.stella.Absyn.ARecordFieldType
import org.syntax.stella.Absyn.AVariantFieldType
import org.syntax.stella.Absyn.SomeTyping
import org.syntax.stella.Absyn.Type
import org.syntax.stella.Absyn.TypeAuto
import org.syntax.stella.Absyn.TypeBool
import org.syntax.stella.Absyn.TypeBottom
import org.syntax.stella.Absyn.TypeForAll
import org.syntax.stella.Absyn.TypeFun
import org.syntax.stella.Absyn.TypeList
import org.syntax.stella.Absyn.TypeNat
import org.syntax.stella.Absyn.TypeRecord
import org.syntax.stella.Absyn.TypeRef
import org.syntax.stella.Absyn.TypeSum
import org.syntax.stella.Absyn.TypeTop
import org.syntax.stella.Absyn.TypeTuple
import org.syntax.stella.Absyn.TypeUnit
import org.syntax.stella.Absyn.TypeVar
import org.syntax.stella.Absyn.TypeVariant
import kotlin.collections.mapValues
import kotlin.collections.List as KtList

sealed class StellaType(private val desc: String, val hasAuto: Boolean) {
    override fun toString(): String {
        return desc
    }

    open fun checkAssignableFrom(that: StellaType) {
        if (that != Bot && that != this) TypeValidationException.errorUnexpectedSubtype(this, that)
    }

    fun concretize(transitions: Map<Int, StellaType>): StellaType = visit { node ->
        when (node) {
            is ForAll -> ForAll(
                node.names,
                node.underlying.concretize(
                    transitions.mapKeys { it.key + node.names.size }
                ))

            is Var -> transitions[node.idx] ?: node
            else -> node
        }
    }

    fun substitute(reconstruction: ReconstructionContext): StellaType = visit { node ->
        when (node) {
            is Auto -> reconstruction.substitute(node)
            else -> node
        }
    }

    fun contains(other: StellaType): Boolean {
        var result = false
        visit {
            if (other == it) result = true
            it
        }
        return result
    }

    fun reshift(context: FunctionContext): StellaType = visit { node ->
        when (node) {
            is Var -> context.checkMapping(node) ?: node
            else -> node
        }
    }

    protected abstract fun visit(block: (StellaType) -> StellaType): StellaType

    object Unknown : StellaType("Unknown", false) {
        override fun visit(block: (StellaType) -> StellaType): StellaType {
            throw UnsupportedOperationException()
        }

    }

    object Nat : StellaType("Nat", false) {
        override fun visit(block: (StellaType) -> StellaType): StellaType = block(this)
    }

    object Unit : StellaType("Unit", false) {
        override fun visit(block: (StellaType) -> StellaType): StellaType = block(this)
    }

    object Bool : StellaType("Bool", false) {
        override fun visit(block: (StellaType) -> StellaType): StellaType = block(this)
    }

    object Top : StellaType("Top", false) {

        override fun visit(block: (StellaType) -> StellaType): StellaType = block(this)

        override fun checkAssignableFrom(that: StellaType) {}
    }

    object Bot : StellaType("Bot", false) {
        override fun visit(block: (StellaType) -> StellaType): StellaType = block(this)
    }

    data class Record(val membersList: KtList<Pair<String, StellaType>>) :
        StellaType("{" + membersList.joinToString { (a, b) -> "$a: $b" } + "}",
            membersList.any { it.second.hasAuto }) {
        val members = membersList.toMap()
        override fun visit(block: (StellaType) -> StellaType): StellaType {
            val n = block(this)
            if (n !== this) return n
            return Record(membersList.map { (k, v) -> k to v.visit(block) })
        }

        override fun checkAssignableFrom(that: StellaType) {
            if (that !is Record) return super.checkAssignableFrom(that)

            for ((k, v) in this.members) {
                val thatV = that.members[k] ?: TypeValidationException.errorMissingRecordFields(k)
                v.checkAssignableFrom(thatV)
            }

        }
    }

    class ForAll(
        val names: KtList<String>,
        val underlying: StellaType,
    ) : StellaType("forall " + names.joinToString(separator = " ") + ". $underlying", underlying.hasAuto) {

        override fun visit(block: (StellaType) -> StellaType): StellaType {
            val n = block(this)
            if (n !== this) return n
            return ForAll(names, underlying.visit(block))
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ForAll

            if (names.size != other.names.size) return false
            if (underlying != other.underlying) return false

            return true
        }

        override fun hashCode(): Int {
            var result = names.size.hashCode()
            result = 31 * result + underlying.hashCode()
            return result
        }

        fun reify(concrete: KtList<StellaType>): StellaType {
            if (this.names.size != concrete.size) {
                TypeValidationException.errorIncorrectNumberOfTypeArguments()
            }

            val mappings = hashMapOf<Int, StellaType>()
            for ((i, t) in concrete.withIndex()) {
                mappings[i] = t
            }

            return underlying.concretize(mappings)
        }

    }

    class Var(
        val name: String,
        val owner: Any,
        val idx: Int
    ) : StellaType("$name ($idx)", false) {

        override fun visit(block: (StellaType) -> StellaType): StellaType = block(this)

        fun shift(v: Int) = Var(name, owner, idx + v)

        operator fun plus(v: Int) = shift(v)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Var

            return idx == other.idx
        }

        override fun hashCode(): Int {
            return idx
        }


    }

    data class Tuple(val members: KtList<StellaType>) :
        StellaType("{" + members.joinToString() + "}", members.any { it.hasAuto }) {

        override fun visit(block: (StellaType) -> StellaType): StellaType {
            val n = block(this)
            if (n !== this) return n
            return Tuple(members.map { it.visit(block) })
        }

        override fun checkAssignableFrom(that: StellaType) {
            if (that !is Tuple) return super.checkAssignableFrom(that)

            if (that.members.size < members.size) return super.checkAssignableFrom(that)

            for ((i, t) in members.withIndex()) {
                t.checkAssignableFrom(that.members[i])
            }
        }
    }

    data class List(val type: StellaType) : StellaType("[$type]", type.hasAuto) {

        override fun visit(block: (StellaType) -> StellaType): StellaType {
            val n = block(this)
            if (n !== this) return n
            return List(type.visit(block))
        }

        override fun checkAssignableFrom(that: StellaType) {
            if (that !is List) return super.checkAssignableFrom(that)
            type.checkAssignableFrom(that.type)
        }
    }

    data class Reference(val underlyingType: StellaType) : StellaType("*$underlyingType", underlyingType.hasAuto) {
        override fun visit(block: (StellaType) -> StellaType): StellaType {
            val n = block(this)
            if (n !== this) return n
            return Reference(underlyingType.visit(block))
        }

        override fun checkAssignableFrom(that: StellaType) {
            if (that !is Reference) return super.checkAssignableFrom(that)

            return underlyingType.checkAssignableFrom(that.underlyingType)
        }
    }

    data class Sum(val a: StellaType, val b: StellaType) : StellaType("$a + $b", a.hasAuto || b.hasAuto) {
        override fun visit(block: (StellaType) -> StellaType): StellaType {
            val n = block(this)
            if (n !== this) return n
            return Sum(a.visit(block), b.visit(block))
        }

        override fun checkAssignableFrom(that: StellaType) {
            if (that !is Sum) return super.checkAssignableFrom(that)

            a.checkAssignableFrom(that.a)
            b.checkAssignableFrom(that.b)
        }
    }

    data class Variant(val membersList: KtList<Pair<String, StellaType?>>) :
        StellaType("<|" + membersList.joinToString { (a, b) -> "$a: $b" } + "|>",
            membersList.any { it.second?.hasAuto == true }) {

        val members = membersList.toMap()

        override fun visit(block: (StellaType) -> StellaType): StellaType {
            val n = block(this)
            if (n !== this) return n
            return Variant(membersList.map { (k, v) -> k to v?.visit(block) })
        }

        override fun checkAssignableFrom(that: StellaType) {
            if (that !is Variant) return super.checkAssignableFrom(that)

            if (this.members.size < that.members.size) TypeValidationException.errorUnexpectedNumberOfParametersInLambda()

            for (k in that.members.keys) {
                val me = this.members[k] ?: TypeValidationException.errorUnexpectedNumberOfParametersInLambda()
                val his = that.members[k]!!

                me.checkAssignableFrom(his)
            }
        }
    }

    data class Fun(val args: KtList<StellaType>, val returnType: StellaType) :
        StellaType("(" + args.joinToString() + ") -> " + returnType, args.any { it.hasAuto } || returnType.hasAuto) {

        override fun visit(block: (StellaType) -> StellaType): StellaType {
            val n = block(this)
            if (n !== this) return n
            return Fun(args.map { it.visit(block) }, returnType.visit(block))
        }

        override fun checkAssignableFrom(that: StellaType) {
            if (that !is Fun) return super.checkAssignableFrom(that)

            if (this.args.size > that.args.size) TypeValidationException.errorUnexpectedNumberOfParametersInLambda()

            that.returnType.checkAssignableFrom(that.returnType)

            for (i in this.args.indices) {
                val me = this.args[i]
                val his = that.args[i]

                his.checkAssignableFrom(me)
            }
        }
    }

    data class Auto(val idx: Int) : StellaType("t$idx", true) {
        override fun visit(block: (StellaType) -> StellaType): StellaType = block(this)
    }

    companion object {
        fun fromAst(t: Type, context: FunctionContext): StellaType {
            return when (t) {
                is TypeNat -> Nat
                is TypeBool -> Bool
                is TypeUnit -> Unit

                is TypeRecord -> Record(t.listrecordfieldtype_.map {
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

                is TypeFun -> Fun(
                    t.listtype_.map { fromAst(it, context) },
                    fromAst(t.type_, context)
                )

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

                is TypeVar -> {
                    context.checkExtension(StellaExtension.UNIVERSAL_TYPES)
                    context.createMapping(t.stellaident_)
                }

                is TypeForAll -> {
                    context.checkExtension(StellaExtension.UNIVERSAL_TYPES)
                    return ForAll(
                        t.liststellaident_,
                        fromAst(t.type_, context.withTypeVariables(t.liststellaident_.toSet(), t))
                    )
                }

                else -> error("Type ${t.javaClass.simpleName} is not supported")
            }
        }
    }

}



