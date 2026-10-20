package dev.varlock.intellij

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class VarlockConfigurable(private val project: Project) : Configurable {
    private val pathField = JBTextField(40)
    private val failLaunch = JBCheckBox("Fail launch when varlock is missing")

    override fun getDisplayName(): String = "varlock"

    override fun createComponent(): JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent("varlock executable path (blank = detect):", pathField)
        .addComponent(failLaunch)
        .panel

    override fun isModified(): Boolean {
        val state = VarlockSettings.getInstance(project).state
        return pathField.text.trim() != state.executablePath || failLaunch.isSelected != state.failLaunchWhenMissing
    }

    override fun apply() {
        val settings = VarlockSettings.getInstance(project)
        settings.state.executablePath = pathField.text.trim()
        settings.state.failLaunchWhenMissing = failLaunch.isSelected
    }

    override fun reset() {
        val state = VarlockSettings.getInstance(project).state
        pathField.text = state.executablePath
        failLaunch.isSelected = state.failLaunchWhenMissing
    }
}
