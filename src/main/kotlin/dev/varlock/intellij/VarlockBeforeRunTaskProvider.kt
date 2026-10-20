package dev.varlock.intellij

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key

/**
 * Opt-in mechanism: the user adds "varlock" to a run configuration's
 * Before Launch list. The platform owns the UI (enable checkbox), persistence
 * (stored in the run configuration XML) and modification tracking — no custom
 * settings editor is involved. The command-line patch hooks
 * ([VarlockPatcher.isConfigured]) key off this task's presence.
 */
class VarlockBeforeRunTaskProvider : BeforeRunTaskProvider<VarlockBeforeRunTaskProvider.Task>() {
    companion object {
        val PROVIDER_ID = Key.create<Task>("dev.varlock.plugin.varlockTask")

        fun isConfigured(profile: com.intellij.execution.configurations.RunConfigurationBase<*>): Boolean =
            profile.beforeRunTasks.any { it.providerId == PROVIDER_ID && it.isEnabled }
    }

    class Task : BeforeRunTask<Task>(PROVIDER_ID)

    override fun getId(): Key<Task> = PROVIDER_ID

    override fun getName(): String = "varlock"

    override fun isConfigurable(): Boolean = false

    override fun createTask(runConfiguration: RunConfiguration): Task = Task()

    override fun executeTask(
        context: DataContext,
        configuration: RunConfiguration,
        environment: ExecutionEnvironment,
        task: Task,
    ): Boolean = VarlockPatcher.validateForLaunch(configuration.project)
}
