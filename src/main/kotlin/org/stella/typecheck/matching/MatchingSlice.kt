package org.stella.typecheck.matching

import org.stella.typecheck.FunctionContext
import org.stella.typecheck.StellaType

class MatchingSlice(
    private val entries: List<PatternModel>
) {

    private val unwrapped = entries.map { it.unwrap() }

    fun checkExhaustive(context: FunctionContext, type: StellaType): Boolean {
        if (entries.any { it is BindingPattern }) return true

        for (pat in entries) {
            pat.checkConforms(context, type)
        }

        return when (type) {
            StellaType.Bool -> checkBool()
            StellaType.Bot -> checkBot()
            is StellaType.Fun -> checkFun()
            is StellaType.List -> checkList()
            StellaType.Nat -> checkNat()
            is StellaType.Reference -> checkReference()
            StellaType.Top -> checkTop()
            StellaType.Unit -> checkUnit()

            is StellaType.Variant -> checkVariant(context, type)
            is StellaType.Record -> checkRecord(context, type)
            is StellaType.Tuple -> checkTuple(context, type)
            is StellaType.Sum -> checkSum(context, type)

            is StellaType.Auto -> false
            is StellaType.ForAll -> false
            is StellaType.Var -> false
            StellaType.Unknown -> throw IllegalStateException("WTF")
        }
    }

    private fun has(block: (PatternModel) -> Boolean) = unwrapped.any { block(it) }

    private fun checkBool() = has { (it as? BoolPattern)?.value == true }
            && has { (it as? BoolPattern)?.value == false }

    private fun checkBot() = true

    private fun checkFun() = false

    private fun checkList() : Boolean {
        val succs = entries.filterIsInstance<ListPattern>()
        val lowest = succs.mapNotNull { it.lower() }.minOrNull() ?: return false
        val constants = succs.mapNotNull { it.constant() }.toSet()

        return verifyRange(lowest, constants)
    }

    private fun checkNat() : Boolean {
        val succs = entries.filterIsInstance<SuccPattern>()
        val lowest = succs.mapNotNull { it.lower() }.minOrNull() ?: return false
        val constants = succs.mapNotNull { it.constant() }.toSet() +
                entries.mapNotNull { (it as? NatPattern)?.value }

        return verifyRange(lowest, constants)
    }

    private fun verifyRange(lowest: Int, constants: Set<Int>): Boolean {
        for (i in 0 until lowest) {
            if (i !in constants) return false
        }

        return true
    }

    private fun checkUnit() = has { it is UnitPattern }

    private fun checkTop() = false

    private fun checkReference() = false

    private fun checkRecord(context: FunctionContext, type: StellaType.Record): Boolean {
        val conforming = unwrapped.filterIsInstance<RecordPattern>()
            .filter { pat ->
                type.members.all { n -> pat.entries.any { it.first == n.key } }
            }
        if (conforming.isEmpty()) return false

        for (name in type.members.keys) {
            val slice = MatchingSlice(conforming.map { it.entries.single { it.first == name }.second })

            if (!slice.checkExhaustive(context, type.members[name]!!)) return false
        }

        return true
    }

    private fun checkSum(context: FunctionContext, type: StellaType.Sum): Boolean {
        val conforming = unwrapped.filterIsInstance<SumPattern>()
        if (conforming.isEmpty()) return false

        val leftSlice = MatchingSlice(conforming.filter { it.left }.map { it.inner })
        val rightSlice = MatchingSlice(conforming.filter { !it.left }.map { it.inner })

        return leftSlice.checkExhaustive(context, type.a) && rightSlice.checkExhaustive(context, type.b)
    }

    private fun checkTuple(context: FunctionContext, type: StellaType.Tuple): Boolean {
        val conforming = unwrapped.filterIsInstance<TuplePattern>()
            .filter { type.members.size == it.patterns.size }
        if (conforming.isEmpty()) return false

        for (idx in type.members.indices) {
            val slice = MatchingSlice(conforming.map { it.patterns[idx] })
            if (!slice.checkExhaustive(context, type.members[idx])) return false
        }

        return true
    }

    private fun checkVariant(context: FunctionContext, type: StellaType.Variant): Boolean {
        val conforming = unwrapped.filterIsInstance<VariantPattern>()
            .filter { pat ->
                type.members.any { it.key == pat.name }
            }
        if (conforming.isEmpty()) return false

        for ((k, v) in type.members) {
            val target = conforming.filter { it.name == k }
            if (target.isEmpty()) return false
            if (v == null) continue

            val slice = MatchingSlice(target.mapNotNull { it.pattern })
            if (!slice.checkExhaustive(context, v)) return false
        }

        return true
    }

}