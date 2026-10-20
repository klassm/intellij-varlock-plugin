package dev.varlock.intellij

object VarlockCommandWrap {
    fun wrap(command: VarlockCommand, exe: String, args: List<String>): Pair<String, List<String>> {
        if (isAlreadyWrapped(exe, args)) return exe to args
        return command.exePath to command.leadingArgs + listOf("run", "--", exe) + args
    }

    fun isAlreadyWrapped(exe: String, args: List<String>): Boolean {
        val exeName = exe.substringAfterLast('/').substringAfterLast('\\')
        val isVarlockExe = exeName.startsWith("varlock")
        val isNpxExe = exeName.startsWith("npx")
        if (!isVarlockExe && !isNpxExe) return false
        if (args.size < 2) return false
        return if (isNpxExe) {
            args[0] == "varlock" && args[1] == "run" && args.getOrNull(2) == "--"
        } else {
            args[0] == "run" && args[1] == "--"
        }
    }
}
