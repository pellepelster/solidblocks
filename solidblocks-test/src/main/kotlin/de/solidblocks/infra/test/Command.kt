class CommandFactory(private val executable: String) {
    fun withArgs() = CommandFactory(this.executable)
}

fun command(executable: String) = CommandFactory(executable)