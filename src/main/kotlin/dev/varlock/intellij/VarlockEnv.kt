package dev.varlock.intellij

import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.nio.file.Path
import java.util.concurrent.TimeUnit

object VarlockEnv {
    /**
     * Runs `varlock load --format json` and returns the resolved environment.
     * The runner is injectable so parsing/loading is testable without the platform.
     */
    fun load(
        command: VarlockCommand,
        workingDir: Path?,
        runner: (List<String>, Path?) -> Pair<Int, String>,
    ): Map<String, String>? {
        val fullArgs = listOf(command.exePath) + command.leadingArgs + listOf("load", "--format", "json")
        val (exitCode, stdout) = runner(fullArgs, workingDir)
        if (exitCode != 0) return null
        return return try {
            parse(stdout)
        } catch (e: Exception) {
            null
        }
    }

    fun parse(stdout: String): Map<String, String> {
        val obj = JsonParser.parseString(stdout).asJsonObject
        return obj.entrySet().associate { it.key to it.value.asString }
    }

    /**
     * Resolves and loads the environment for a project launch.
     * @throws com.intellij.execution.CantRunException when varlock is missing and fail-launch is enabled.
     */
    fun loadForProject(project: Project): Map<String, String>? {
        val settings = VarlockSettings.getInstance(project).state
        val command = VarlockExecutableResolver.resolve(
            settings.executablePath,
            project.basePath?.let { Path.of(it) },
            { name -> com.intellij.execution.configurations.PathEnvironmentVariableUtil.findInPath(name)?.toPath() },
        )
        if (command == null) {
            if (settings.failLaunchWhenMissing) {
                throw com.intellij.execution.CantRunException(
                    "varlock executable not found. Install varlock or set its path in Settings → Tools → varlock."
                )
            }
            return null
        }
        val (exitCode, stdout) = runProcess(
            listOf(command.exePath) + command.leadingArgs + listOf("load", "--format", "json"),
            project.basePath?.let { Path.of(it) },
        )
        if (exitCode != 0) {
            Logger.getInstance("varlock").warn("varlock load failed with exit code $exitCode")
            return null
        }
        return try {
            parse(stdout)
        } catch (e: Exception) {
            Logger.getInstance("varlock").warn("varlock load output unparseable", e)
            null
        }
    }

    private fun runProcess(args: List<String>, workingDir: Path?): Pair<Int, String> {
        val pb = ProcessBuilder(args).redirectErrorStream(false)
        workingDir?.let { pb.directory(it.toFile()) }
        val proc = pb.start()
        val stdout = proc.inputStream.readBytes().toString(Charsets.UTF_8)
        val finished = proc.waitFor(60, TimeUnit.SECONDS)
        if (!finished) {
            proc.destroyForcibly()
            return -1 to ""
        }
        return proc.exitValue() to stdout
    }
}
