package dev.varlock.intellij

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VarlockCommandWrapTest {
    private val cmd = VarlockCommand("/opt/homebrew/bin/varlock", emptyList())

    @Test
    fun `wraps exe and args with run and separator`() {
        val (exe, args) = VarlockCommandWrap.wrap(cmd, "node", listOf("server.js", "--port 3000"))
        assertEquals("/opt/homebrew/bin/varlock", exe)
        assertEquals(listOf("run", "--", "node", "server.js", "--port 3000"), args)
    }

    @Test
    fun `args containing spaces stay single args`() {
        val (_, args) = VarlockCommandWrap.wrap(cmd, "npm", listOf("run", "build -- --watch"))
        assertEquals(listOf("run", "--", "npm", "run", "build -- --watch"), args)
    }

    @Test
    fun `npx command keeps its leading args`() {
        val npx = VarlockCommand("/usr/bin/npx", listOf("--yes", "varlock"))
        val (exe, args) = VarlockCommandWrap.wrap(npx, "node", listOf("app.js"))
        assertEquals("/usr/bin/npx", exe)
        assertEquals(listOf("--yes", "varlock", "run", "--", "node", "app.js"), args)
    }

    @Test
    fun `already wrapped command is not wrapped twice`() {
        val (_, args) = VarlockCommandWrap.wrap(cmd, "/opt/homebrew/bin/varlock", listOf("run", "--", "node", "app.js"))
        assertEquals(listOf("run", "--", "node", "app.js"), args)
    }

    @Test
    fun `npx form is idempotent`() {
        val npx = VarlockCommand("/usr/bin/npx", listOf("varlock"))
        val (_, args) = VarlockCommandWrap.wrap(npx, "/usr/bin/npx", listOf("varlock", "run", "--", "node", "app.js"))
        assertEquals(listOf("varlock", "run", "--", "node", "app.js"), args)
    }

    @Test
    fun `windows script names are idempotent`() {
        val (_, args) = VarlockCommandWrap.wrap(cmd, "C:\\bin\\varlock.cmd", listOf("run", "--", "node", "app.js"))
        assertEquals(listOf("run", "--", "node", "app.js"), args)
    }

    @Test
    fun `empty args wrap cleanly`() {
        val (exe, args) = VarlockCommandWrap.wrap(cmd, "sh", emptyList())
        assertEquals("/opt/homebrew/bin/varlock", exe)
        assertEquals(listOf("run", "--", "sh"), args)
    }
}
