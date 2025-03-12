package org.stella.typecheck

import org.stella.typecheck.visitor.ProgramVisitor
import org.syntax.stella.Absyn.AProgram
import org.syntax.stella.Absyn.DeclFun
import org.syntax.stella.Absyn.DeclTypeAlias
import org.syntax.stella.Absyn.Program

object TypeCheck {
    @Throws(Exception::class)
    fun typecheckProgram(program: Program) {
        program as AProgram
        checkMain(program)
        program.accept(ProgramVisitor(), Unit)
    }

    private fun checkMain(program: AProgram) {
        program.listdecl_.filterIsInstance<DeclFun>()
            .singleOrNull { it.stellaident_ == "main" }
    }
}
