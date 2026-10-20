package dev.varlock.intellij

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.project.Project

@State(name = "VarlockSettings", storages = [Storage("varlock.xml")])
@Service(Service.Level.PROJECT)
class VarlockSettings : PersistentStateComponent<VarlockSettings.State> {
    class State {
        var executablePath: String = ""
        var failLaunchWhenMissing: Boolean = true
    }

    private var state = State()

    override fun getState(): State = state
    override fun loadState(s: State) {
        state = s
    }

    companion object {
        fun getInstance(project: Project): VarlockSettings = project.getService(VarlockSettings::class.java)
    }
}
