package com.romdoctor.core.shell

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellCommandTest {
    @Test
    fun rootScriptQuotesEveryArgument() {
        assertEquals("cat '/proc/mounts'", cmd("cat", "/proc/mounts").toRootScript())
        assertEquals("cat 'a'\\''b'", cmd("cat", "a'b").toRootScript())
        assertEquals("cat '; reboot'", cmd("cat", "; reboot").toRootScript())
    }

    @Test
    fun invalidBinaryIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { cmd("../bin/sh") }
        assertThrows(IllegalArgumentException::class.java) { cmd("ls; reboot") }
    }

    @Test
    fun nulInArgumentIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { cmd("cat", "a\u0000b") }
    }
}

class IdParserTest {
    @Test
    fun realMagiskOutputIsRoot() {
        assertTrue(IdParser.isUidZero("uid=0(root) gid=0(root) groups=0(root) context=u:r:magisk:s0"))
    }

    @Test
    fun shellUserIsNotRoot() {
        assertFalse(IdParser.isUidZero("uid=2000(shell) gid=2000(shell) groups=2000(shell)"))
        assertFalse(IdParser.isUidZero("uid=10123(u0_a123) gid=10123"))
        assertFalse(IdParser.isUidZero(""))
    }
}

class ProcessShellExecutorTest {
    private val exec = ProcessShellExecutor(whitelist = setOf("echo", "sleep", "false"))

    @Test
    fun capturesStdoutAndExitCode() = runBlocking {
        val r = exec.run(cmd("echo", "hello"))
        assertTrue(r.ok)
        assertEquals("hello", r.stdout.trim())
        assertEquals(0, r.exitCode)
    }

    @Test
    fun nonZeroExitIsNotOk() = runBlocking {
        val r = exec.run(cmd("false"))
        assertFalse(r.ok)
        assertEquals(1, r.exitCode)
    }

    @Test
    fun timeoutKillsProcess() = runBlocking {
        val r = exec.run(cmd("sleep", "10"), timeoutMs = 300)
        assertTrue(r.timedOut)
        assertNull(r.exitCode)
        assertTrue(r.durationMs < 5_000)
    }

    @Test
    fun nonWhitelistedBinaryIsBlocked() = runBlocking {
        val r = exec.run(cmd("reboot"))
        assertFalse(r.ok)
        assertTrue(r.startError!!.contains("whitelist"))
    }
}
