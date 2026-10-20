package dev.varlock.intellij

import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import org.jdom.Element

class VarlockRunConfigurationExtension : RunConfigurationExtension() {
    /**
     * Opt-in is expressed by a "varlock" Before Launch task on the run
     * configuration (see VarlockBeforeRunTaskProvider); no per-config XML state
     * or custom settings editor is used.
     */
    override fun isApplicableFor(profile: RunConfigurationBase<*>): Boolean = true

    override fun isEnabledFor(applicableConfiguration: RunConfigurationBase<*>, runnerSettings: RunnerSettings?): Boolean =
        VarlockBeforeRunTaskProvider.isConfigured(applicableConfiguration)

    /**
     * Invoked for external-system run configurations (Maven/Gradle client) in 2024.2.
     * Application/JUnit launches build their command line from JavaParameters (which
     * offers no executable hook) and are not wrappable on this platform version.
     */
    override fun patchCommandLine(
        configuration: RunConfigurationBase<*>,
        runnerSettings: RunnerSettings?,
        cmdLine: GeneralCommandLine,
        runnerId: String,
    ) {
        if (!isEnabledFor(configuration, runnerSettings)) return
        VarlockPatcher.wrap(cmdLine, configuration.project)
    }

    /**
     * Java-family launches (Application, JUnit) cannot be command-wrapped on this
     * platform version; inject the resolved varlock environment instead.
     */
    override fun <P : RunConfigurationBase<*>> updateJavaParameters(
        configuration: P,
        params: JavaParameters,
        runnerSettings: RunnerSettings?,
    ) {
        if (!VarlockBeforeRunTaskProvider.isConfigured(configuration)) return
        val env = VarlockEnv.loadForProject(configuration.project) ?: return
        params.env.putAll(env)
    }
}
