package com.romdoctor.core.shell

interface ShellExecutor {
    suspend fun run(
        command: ShellCommand,
        asRoot: Boolean = false,
        timeoutMs: Long = 10_000,
    ): ShellResult
}
