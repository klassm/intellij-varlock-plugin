package dev.varlock.intellij

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class VarlockFailLaunchTest {
    @Test
    fun `resolver returning null yields null command`() {
        assertNull(VarlockExecutableResolver.resolve("", null, { null }))
    }

    @Test
    fun `unresolvable and failLaunch means wrap must be refused`() {
        assertThrows(VarlockPatcher.VarlockMissingException::class.java) {
            VarlockPatcher.requireResolved(null, failLaunch = true)
        }
    }

    @Test
    fun `unresolvable and no failLaunch returns null without throwing`() {
        assertNull(VarlockPatcher.requireResolved(null, failLaunch = false))
    }

    @Test
    fun `resolved command passes through unchanged`() {
        val cmd = VarlockCommand("/opt/homebrew/bin/varlock", emptyList())
        assert(VarlockPatcher.requireResolved(cmd, failLaunch = true) === cmd)
    }
}
