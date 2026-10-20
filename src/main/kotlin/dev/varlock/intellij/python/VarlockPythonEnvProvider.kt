package dev.varlock.intellij.python

import com.intellij.execution.CantRunException
import com.intellij.openapi.project.Project
import com.jetbrains.python.run.AbstractPythonRunConfiguration
import com.jetbrains.python.run.PythonExecution
import com.jetbrains.python.run.PythonRunParams
import com.jetbrains.python.run.target.HelpersAwareTargetEnvironmentRequest
import com.jetbrains.python.run.target.PythonCommandLineTargetEnvironmentProvider
import dev.varlock.intellij.VarlockBeforeRunTaskProvider
import dev.varlock.intellij.VarlockEnv

/**
 * Python run configurations (including uv-managed interpreters) have no
 * command-wrap hook; the resolved varlock environment is injected instead.
 * Note: this gives varlock's env resolution/validation but no output redaction.
 */
@Suppress("UnstableApiUsage")
class VarlockPythonEnvProvider : PythonCommandLineTargetEnvironmentProvider {
    override fun extendTargetEnvironment(
        project: Project,
        request: HelpersAwareTargetEnvironmentRequest,
        pythonExecution: PythonExecution,
        pythonRunParams: PythonRunParams,
    ) {
        val config = pythonRunParams as? AbstractPythonRunConfiguration<*> ?: return
        if (!VarlockBeforeRunTaskProvider.isConfigured(config)) return
        val env = try {
            VarlockEnv.loadForProject(project)
        } catch (e: CantRunException) {
            throw e
        } ?: return
        env.forEach { (key, value) -> pythonExecution.addEnvironmentVariable(key, value) }
    }
}
