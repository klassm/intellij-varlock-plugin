package dev.varlock.intellij

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

class VarlockExecutableResolverTest {
    @TempDir
    lateinit var dir: Path

    private fun makeExecutable(path: Path): Path {
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"))
        return path
    }

    @Test
    fun `explicit configured path wins when executable`() {
        val fake = makeExecutable(Files.createFile(dir.resolve("my-varlock")))
        val cmd = VarlockExecutableResolver.resolve(fake.toString(), null)
        assertEquals(fake.toString(), cmd?.exePath)
        assertEquals(emptyList<String>(), cmd?.leadingArgs)
    }

    @Test
    fun `configured path ignored when not executable`() {
        val cmd = VarlockExecutableResolver.resolve(dir.resolve("missing").toString(), null)
        assertNull(cmd)
    }

    @Test
    fun `project node_modules bin is used`() {
        val bin = Files.createDirectories(dir.resolve("node_modules/.bin"))
        val fake = makeExecutable(Files.createFile(bin.resolve("varlock")))
        val cmd = VarlockExecutableResolver.resolve("", dir)
        assertEquals(fake.toString(), cmd?.exePath)
    }

    @Test
    fun `npx fallback carries varlock as leading args with yes flag`() {
        val cmd = VarlockExecutableResolver.fromNpx("/usr/bin/npx")
        assertEquals("/usr/bin/npx", cmd.exePath)
        assertEquals(listOf("--yes", "varlock"), cmd.leadingArgs)
    }

    @Test
    fun `path lookup finds varlock before npx`() {
        val onPath = makeExecutable(Files.createFile(dir.resolve("varlock")))
        val npx = makeExecutable(Files.createFile(dir.resolve("npx")))
        val cmd = VarlockExecutableResolver.resolve("", null, { name -> dir.resolve(name) })
        assertEquals(onPath.toString(), cmd?.exePath)
        assertEquals(emptyList<String>(), cmd?.leadingArgs)
        val cmd2 = VarlockExecutableResolver.resolve("", null, { name ->
            if (name == "npx") npx else null
        })
        assertEquals(npx.toString(), cmd2?.exePath)
        assertEquals(listOf("--yes", "varlock"), cmd2?.leadingArgs)
    }

    @Test
    fun `nothing found yields null`() {
        assertNull(VarlockExecutableResolver.resolve("", null, { null }))
    }

    @Test
    fun `directory named varlock does not resolve`() {
        Files.createDirectories(dir.resolve("varlock"))
        assertNull(VarlockExecutableResolver.resolve("", dir, { name -> dir.resolve(name) }))
    }

    @Test
    fun `relative configured path resolves against project dir`() {
        val bin = Files.createDirectories(dir.resolve("node_modules/.bin"))
        val fake = makeExecutable(Files.createFile(bin.resolve("varlock")))
        val cmd = VarlockExecutableResolver.resolve("node_modules/.bin/varlock", dir)
        assertEquals(fake.toString(), cmd?.exePath)
    }
}
