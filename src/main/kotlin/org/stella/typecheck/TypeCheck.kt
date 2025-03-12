package org.stella.typecheck

import org.syntax.stella.Absyn.AProgram
import org.syntax.stella.Absyn.DeclFun
import org.syntax.stella.Absyn.DeclTypeAlias
import org.syntax.stella.Absyn.Program

object TypeCheck {
    @Throws(Exception::class)
    fun typecheckProgram(program: Program) {
        when (program) {
            is AProgram -> program.listdecl_.map {
                when (it) {
                    is DeclFun -> {
                        val name = it.stellaident_
                        println("Declared function $name")
                    }
                    is DeclTypeAlias -> {}
                }
            }

        }
    }
}
