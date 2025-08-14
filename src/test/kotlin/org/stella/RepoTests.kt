package org.stella

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

    @ParameterizedTest(name = "{index} Typechecking ill-typed program {0}")
    @MethodSource("illTypedPathStream")
    @Throws(
        IOException::class,
        Exception::class
    )
    fun testIllTyped(input: Pair<StellaExceptionCode, Path>) {
        val expectedCode = input.first
        val filepath = input.second
        println(filepath.toUri())
        val original = System.`in`
        val fips = FileInputStream(filepath.toFile())
        System.setIn(fips)
        val thrown = assertThrows<TypeValidationException>("expected the typechecker to fail!") {
            main()
        }
        assertEquals(expectedCode, thrown.code, thrown.message)
        System.setIn(original)
    }

    companion object {
        private const val BASE_DIR = "third_party/stella-tests"
        private const val OK_TESTS = "$BASE_DIR/ok"
        private const val BAD_TESTS = "$BASE_DIR/bad"

        @JvmStatic
        fun wellTypedPathStream(): Stream<Path> = getFilesStream(OK_TESTS)

        @JvmStatic
        fun illTypedPathStream(): Stream<Pair<StellaExceptionCode, Path>> {
            return Files.list(Paths.get(BAD_TESTS))
                .filter {
                    StellaExceptionCode.values().any { code -> it.name == code.name }
                }
                .flatMap {
                    val code = StellaExceptionCode.valueOf(it.name)
                    Files.list(it).map { code to it }
                }.toList().stream()
        }

        private fun getFilesStream(path: String) = Files.list(Paths.get(path))
    }
}