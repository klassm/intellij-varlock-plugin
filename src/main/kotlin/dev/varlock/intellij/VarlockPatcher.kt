package dev.varlock.intellij

import com.intellij.execution.CantRunException
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import java.nio.file.Path

object VarlockPatcher {
    class VarlockMissingException : RuntimeException()

    const val RUN_MARKER_ENV = "__VARLOCK_RUN"

    fun requireResolved(cmd: VarlockCommand?, failLaunch: Boolean): VarlockCommand? {
        if (cmd != null) return cmd
        if (failLaunch) throw VarlockMissingException()
        return null
    }

    /**
     * Validates varlock availability at Before-Launch time; aborts the launch with a
     * clear error when the executable is missing and fail-launch is enabled.
     */
    fun validateForLaunch(project: Project): Boolean {
        val state = VarlockSettings.getInstance(project).state
        val resolved = VarlockExecutableResolver.resolve(
            state.executablePath,
            project.basePath?.let { Path.of(it) },
            { name -> PathEnvironmentVariableUtil.findInPath(name)?.toPath() },
        )
        return try {
            requireResolved(resolved, state.failLaunchWhenMissing) != null
        } catch (e: VarlockMissingException) {
            throw CantRunException(
                "varlock executable not found. Install varlock or set its path in Settings → Tools → varlock.",
                e,
            )
        }
    }

    /**
     * Resolves the varlock executable for a launch.
     * @throws VarlockMissingException when the executable cannot be resolved and fail-launch is enabled.
     * @return null when unresolvable and fail-launch is disabled (a warning is shown).
     */
    fun resolveForLaunch(project: Project): VarlockCommand? {
        val state = VarlockSettings.getInstance(project).state
        val resolved = VarlockExecutableResolver.resolve(
            state.executablePath,
            project.basePath?.let { Path.of(it) },
            { name -> PathEnvironmentVariableUtil.findInPath(name)?.toPath() },
        )
        return try {
            requireResolved(resolved, state.failLaunchWhenMissing)
        } catch (e: VarlockMissingException) {
            throw CantRunException(
                "varlock executable not found. Install varlock or set its path in Settings → Tools → varlock.",
                e,
            )
        }
    }

    fun wrap(cmdLine: GeneralCommandLine, project: Project) {
        if (cmdLine.environment.containsKey(RUN_MARKER_ENV)) return
        val resolved = resolveForLaunch(project) ?: return
        val oldExe = cmdLine.exePath
        val oldArgs = cmdLine.parametersList.parameters.toList()
        val (newExe, newArgs) = VarlockCommandWrap.wrap(resolved, oldExe, oldArgs)
        cmdLine.exePath = newExe
        cmdLine.parametersList.clearAll()
        newArgs.forEach { cmdLine.parametersList.add(it) }
        cmdLine.environment[RUN_MARKER_ENV] = "1"
    }
}
