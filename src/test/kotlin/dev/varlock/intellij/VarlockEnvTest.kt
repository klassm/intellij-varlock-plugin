package dev.varlock.intellij

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VarlockEnvTest {
    private val varlock = VarlockCommand("/opt/homebrew/bin/varlock", emptyList())

    @Test
    fun `parses varlock load json output into env map`() {
        val env = VarlockEnv.load(varlock, null, { _, _ ->
            0 to """{"PORT":"8080","DB_URL":"postgres://x","TOKEN":"s3cr3t"}"""
        })
        assertEquals(mapOf("PORT" to "8080", "DB_URL" to "postgres://x", "TOKEN" to "s3cr3t"), env)
    }

    @Test
    fun `passes load format json arguments and working dir to runner`() {
        var capturedArgs: List<String>? = null
        var capturedDir: java.nio.file.Path? = null
        VarlockEnv.load(varlock, java.nio.file.Path.of("/proj"), { args, dir ->
            capturedArgs = args
            capturedDir = dir
            0 to "{}"
        })
        assertEquals(
            listOf("/opt/homebrew/bin/varlock", "load", "--format", "json"),
            capturedArgs,
        )
        assertEquals(java.nio.file.Path.of("/proj"), capturedDir)
    }

    @Test
    fun `non-zero exit yields null`() {
        assertNull(VarlockEnv.load(varlock, null, { _, _ -> 1 to "boom" }))
    }

    @Test
    fun `invalid json yields null`() {
        assertNull(VarlockEnv.load(varlock, null, { _, _ -> 0 to "not json" }))
    }

    @Test
    fun `values with equals signs survive parsing`() {
        val env = VarlockEnv.load(varlock, null, { _, _ -> 0 to """{"FLAG":"a=b=c"}""" })
        assertEquals("a=b=c", env?.get("FLAG"))
    }
}
