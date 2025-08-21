package org.stella

import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.stella.typecheck.StellaExceptionCode
import org.stella.typecheck.TypeValidationException
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.io.path.name
import kotlin.test.assertEquals

internal class RepoTests {
    @ParameterizedTest(name = "{index} Typechecking well-typed program {0}")
    @MethodSource("wellTypedPathStream")
    @Throws(
        IOException::class,
        Exception::class
    )
    fun testWellTyped(filepath: Path) {
        println(filepath.toUri())
        val original = System.`in`
        val fips = FileInputStream(filepath.toFile())
        System.setIn(fips)
        main()
        System.setIn(original)
    }

    private fun testIllTyped(expectedCodeStr: String, filepath: Path) {
        val expectedCode = StellaExceptionCode.valueOf(expectedCodeStr)
        println(filepath.toUri())
        val original = System.`in`
        val fips = FileInputStream(filepath.toFile())
        System.setIn(fips)
        val thrown = assertThrows<TypeValidationException>("expected the typechecker to fail!") {
            main()
        }
        //assertEquals(expectedCode, thrown.code, thrown.message)
        System.setIn(original)
    }

    private fun createIllTest(name: String, filepath: Path): DynamicTest {
        return DynamicTest.dynamicTest(
            filepath.name
        ) {
            testIllTyped(name, filepath)
        }
    }

    @TestFactory
    fun illTyped(): List<DynamicNode> {
        val codeFolders = Files.list(Paths.get(BAD_TESTS))

        return codeFolders.map { codeFolder ->
            val name = codeFolder.name
            val nodes = Files.list(codeFolder).map {
                createIllTest(name, it)
            }
            DynamicContainer.dynamicContainer(name, nodes)
        }.toList()
    }

    companion object {
        private const val BASE_DIR = "third_party/stella-tests"
        private const val OK_TESTS = "$BASE_DIR/ok"
        private const val BAD_TESTS = "$BASE_DIR/bad"

        @JvmStatic
        fun wellTypedPathStream(): Stream<Path> = getFilesStream(OK_TESTS)


        private fun getFilesStream(path: String) = Files.list(Paths.get(path))
    }
}