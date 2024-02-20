package org.ldemetrios.khell

import kotlinx.coroutines.channels.*
import org.ldemetrios.khell.sugar.*
import org.ldemetrios.utilities.TerminalException
import org.ldemetrios.utilities.errno
import org.ldemetrios.utilities.exec
import org.ldemetrios.utilities.toOnelineCommand

fun terminal(vararg command: String, verboseCommandInException: Boolean = true) =
    UnsafeFlange<String, String, String, TerminalException> {
        val ( output, error, ret) = exec(
            command.toList(),
            input = input.toList().joinToString(System.lineSeparator())
        )
        for (e in error) sendError(e)
        for (o in output) send(o)
        if (ret != 0) throw TerminalException(
            ret,
            "Command ${
                if (verboseCommandInException) toOnelineCommand(command.toList()) else ""
            } failed with return code $ret (Probably means '${errno(ret)}')"
        )
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

