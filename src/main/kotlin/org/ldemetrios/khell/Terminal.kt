package org.ldemetrios.khell

import kotlinx.coroutines.channels.*
import org.ldemetrios.khell.sugar.*
import org.ldemetrios.utilities.constants.NEWLINES
import java.io.BufferedReader
import java.io.InputStreamReader

class TerminalException internal constructor(val errorCode: Int, message: String) : RuntimeException(message)

private const val CR_CODE = '\r'.code
private const val LF_CODE = '\n'.code
private val NEWLINE_CODES = NEWLINES.map(Char::code)

class LinedOutput(val channel: SendChannel<String>) {
    private val buffer = java.util.ArrayDeque<String>()
    private var sb = StringBuilder()
    private var rPassed = false
    private fun send() {
        buffer.addLast(sb.toString())
        sb.clear()
    }

    fun write(p0: Int) {
        if (rPassed) {
            send()
            when (p0) {
                LF_CODE -> Unit
                CR_CODE -> Unit
                in NEWLINE_CODES -> /*One more*/ send()
                else -> sb.append(p0.toChar())
            }
        } else {
            when (p0) {
                CR_CODE -> Unit
                /*LF or*/ in NEWLINE_CODES -> send()
                else -> sb.append(p0.toChar())
            }
        }
        rPassed = p0 == CR_CODE
    }

    fun close() {
        if (sb.isNotEmpty()) {
            buffer.add(sb.toString())
        }
    }

    fun tryFlush() {
        while (buffer.isNotEmpty()) {
            val line = buffer.removeFirst()
            val res = channel.trySend(line)
            when {
                res.isSuccess -> Unit
                res.isFailure -> {
                    buffer.addFirst(line)
                    break
                }

                res.isClosed -> throw ClosedSendChannelException("Unable to send line to channel")
            }
        }
    }

    suspend fun sendBlocking() {
        if (buffer.isNotEmpty()) channel.send(buffer.removeFirst())
    }

    suspend fun sendAll() {
        close()
        while (buffer.isNotEmpty()) channel.send(buffer.removeFirst())
    }
}

fun terminal(vararg command: String, verboseCommandInException: Boolean = true) =
    UnsafeFlange<String, String, String, TerminalException> {

        val builder = ProcessBuilder(*command)
        val process = builder.start()

        val output = this.output?.let { LinedOutput(it) }
        val error = this.error?.let { LinedOutput(it) }

        val processOutput = BufferedReader(InputStreamReader(process.inputStream))
        val processInput = process.outputStream
        val processError = BufferedReader(InputStreamReader(process.errorStream))


        fun flushInput() {
            do {
                val result = input.tryReceive()
                result.onSuccess {
                    process.outputStream.write(it.toByteArray())
                    process.outputStream.write(System.lineSeparator().toByteArray())
                }
            } while (result.isSuccess)
        }

        fun flushOutgoing(thisOut: LinedOutput?, processOut: BufferedReader) {
            if (thisOut != null) {
                while (processOut.ready()) {
                    thisOut.write(processOut.read())
                }
                thisOut.tryFlush() // Expected not to block
            }
        }

        fun flushOutput() {
            flushOutgoing(output, processOutput)
        }

        fun flushError() {
            flushOutgoing(error, processError)
        }

        // Anything ready, without blocking
        flushError()
        flushOutput()
        flushInput()
        flushError()
        flushOutput()

        while (process.isAlive) {
            // Receive blocking
            val tryReceive = input.receiveCatching()
            if (tryReceive.isSuccess) {
                processInput.write(tryReceive.getOrThrow().toByteArray())
                processInput.write(System.lineSeparator().toByteArray())
            }
            if (tryReceive.isClosed) {
                processInput.close()
                break
            }
            Thread.sleep(50)
            flushError()
            flushOutput()

            output?.sendBlocking()
        }

        val exitValue = process.waitFor()
        if (error != null) {
            var ch: Int
            while (processError.read().also { ch = it } != -1) {
                error.write(ch)
            }
            error.sendAll() // Expected not to block
        }
        if (output != null) {
            var ch: Int
            while (processOutput.read().also { ch = it } != -1) {
                output.write(ch)
            }
            output.sendAll()
        }
        if (exitValue != 0) {
            if (verboseCommandInException) {
                throw TerminalException(
                    exitValue,
                    "Command ${command.joinToString(" ")} failed with exit code $exitValue"
                )
            } else {
                throw TerminalException(exitValue, "Command failed with exit code $exitValue")
            }
        }
    }


fun main() {
    `$`.unsafe<IllegalArgumentException>().dirty<Int>()() {
        send(2)
        echo_n / beware<IllegalArgumentException>() / /*dirtyAuto*/ {
            send(3)
            sendError(4)
//            throw IllegalArgumentException()
            send(5)
            send(6)
            send(7)
            send(8)
        } / `!` / /*unsafe<IllegalArgumentException>()()*/ {
            send(receive().let { it * it })
            send(receive().let { it * it })
            send(receive().let { it * it })
        } / cat
        send(8)
        send(10)
    } / redirect(Out, null) {
        send(receive().let { it * it })
        sendError(6) // Redirected to null
        send(receive().let { it * it })
        send(receive().let { it * it })
    } / join() / xargs1(::println) / Collect - ::println

    println()

    terminal("ls", "-Ali") / terminal("tail", "-n", "+2") / xargs1(::println) / Collect // - ::println

    println()

    1 `»` { it + 2 } `»` { it * 2 } `»` ::println

    println()

    1 - 2::plus - 2::times - ::println

    println()

    terminal("touch", "/ha-ha.txt") / CollectSafe - ::println
}

