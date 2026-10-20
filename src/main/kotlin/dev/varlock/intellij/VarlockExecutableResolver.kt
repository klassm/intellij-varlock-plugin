package dev.varlock.intellij

import java.nio.file.Files
import java.nio.file.Path

data class VarlockCommand(val exePath: String, val leadingArgs: List<String>)

object VarlockExecutableResolver {
    fun resolve(
        executablePath: String,
        projectPath: Path?,
        lookupInPath: (String) -> Path? = { null },
    ): VarlockCommand? {
        executablePath.takeIf { it.isNotBlank() }?.let {
            val p = Path.of(it)
            val resolved = if (p.isAbsolute || projectPath == null) p else projectPath.resolve(p)
            if (Files.isRegularFile(resolved) && Files.isExecutable(resolved)) {
                return VarlockCommand(resolved.toString(), emptyList())
            }
        }
        lookupInPath("varlock")?.let { if (Files.isRegularFile(it) && Files.isExecutable(it)) return VarlockCommand(it.toString(), emptyList()) }
        projectPath?.resolve("node_modules/.bin/varlock")?.let {
            if (Files.isRegularFile(it) && Files.isExecutable(it)) return VarlockCommand(it.toString(), emptyList())
        }
        lookupInPath("npx")?.let { if (Files.isRegularFile(it) && Files.isExecutable(it)) return fromNpx(it.toString()) }
        return null
    }

    fun fromNpx(npxPath: String): VarlockCommand = VarlockCommand(npxPath, listOf("--yes", "varlock"))
}
