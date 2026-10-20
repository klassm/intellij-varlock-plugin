package dev.varlock.intellij.nodejs

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.target.value.TargetValue
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile
import com.intellij.javascript.nodejs.execution.NodeTargetRun
import com.intellij.javascript.nodejs.execution.runConfiguration.AbstractNodeRunConfigurationExtension
import com.intellij.javascript.nodejs.execution.runConfiguration.NodeRunConfigurationLaunchSession
import com.intellij.lang.javascript.buildTools.npm.rc.NpmRunConfiguration
import com.intellij.openapi.options.SettingsEditor
import com.intellij.ui.components.JBLabel
import com.jetbrains.nodejs.run.NodeJsRunConfiguration
import dev.varlock.intellij.VarlockBeforeRunTaskProvider
import dev.varlock.intellij.VarlockPatcher
import javax.swing.JComponent

class VarlockNodeRunConfigurationExtension : AbstractNodeRunConfigurationExtension() {
    override fun isApplicableFor(profile: AbstractNodeTargetRunProfile): Boolean =
        profile is NodeJsRunConfiguration || profile is NpmRunConfiguration

    /**
     * Wrapping happens through the launch session: the platform invokes
     * createLaunchSession for Node/npm launches and the session's
     * addNodeOptionsTo rewrites the target command line so the launch becomes
     * `varlock run -- <node …>`. Opt-in is keyed off the Before Launch task.
     */
    override fun createLaunchSession(
        profile: AbstractNodeTargetRunProfile,
        environment: ExecutionEnvironment,
    ): NodeRunConfigurationLaunchSession {
        return VarlockNodeLaunchSession(profile)
    }

    /**
     * No settings UI: opt-in happens in the Before Launch list. Returning an
     * empty fragment list prevents the platform from rendering any tab.
     */
    override fun <P : AbstractNodeTargetRunProfile> createFragments(configuration: P): List<SettingsEditor<P>>? =
        emptyList()

    override fun <P : AbstractNodeTargetRunProfile> createEditor(configuration: P): SettingsEditor<P> {
        @Suppress("UNCHECKED_CAST")
        return VarlockHintEditor() as SettingsEditor<P>
    }

    override fun getEditorTitle(): String? = "varlock"
}

/** Informative tab: varlock is opted in via the Before Launch list, not here. */
class VarlockHintEditor : SettingsEditor<AbstractNodeTargetRunProfile>() {
    override fun createEditor(): JComponent = JBLabel(
        "Run with varlock: add 'varlock' to this configuration's 'Before Launch' list.",
    )

    override fun resetEditorFrom(s: AbstractNodeTargetRunProfile) {}

    override fun applyEditorTo(s: AbstractNodeTargetRunProfile) {}
}

class VarlockNodeLaunchSession(private val profile: AbstractNodeTargetRunProfile) : NodeRunConfigurationLaunchSession() {
    override fun addNodeOptionsTo(nodeTargetRun: NodeTargetRun) {
        if (!VarlockBeforeRunTaskProvider.isConfigured(profile)) {
            super.addNodeOptionsTo(nodeTargetRun)
            return
        }
        try {
            val builder = nodeTargetRun.commandLineBuilder
            val originalExe: TargetValue<String> = builder.exePath
            val resolved = VarlockPatcher.resolveForLaunch(profile.project)
            if (resolved == null) {
                super.addNodeOptionsTo(nodeTargetRun)
                return
            }
            // The original exe may be an async TargetValue (e.g. nvm interpreter
            // resolution) — carry it through as a parameter instead of resolving it.
            val prefix: List<TargetValue<String>> =
                (resolved.leadingArgs + listOf("run", "--")).map { TargetValue.fixed(it) }
            builder.addParametersAt(0, prefix + listOf(originalExe))
            builder.exePath = TargetValue.fixed(resolved.exePath)
        } catch (t: Throwable) {
            com.intellij.openapi.diagnostic.Logger.getInstance("varlock").error("varlock node wrap failed", t)
            super.addNodeOptionsTo(nodeTargetRun)
        }
    }
}
