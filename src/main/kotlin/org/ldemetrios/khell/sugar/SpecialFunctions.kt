package org.ldemetrios.khell.sugar

import org.ldemetrios.khell.SafeFlange
import org.ldemetrios.khell.UnsafeFlange
import org.ldemetrios.utilities.ArityException

////////////////////////////////////// Shell begin

fun <I> echo(vararg elements: I) = SafeFlange<Nothing, I, Nothing> {
    for (e in elements) send(e)
}

val echo_n = SafeFlange<Nothing, Nothing, Nothing> { }

////////////////////////////////////// Joining

fun <I> join() = SafeFlange<I, List<I>, Nothing> {
    send(collect())
}

fun <I> flatten() = SafeFlange<List<I>, I, Nothing> {
    for (list in input) {
        for (e in list) send(e)
    }
}

fun <I, O> xargs(f: (List<I>) -> O) = SafeFlange<I, O, Nothing> {
    send(f(collect()))
}

fun <I, O> xargs1(f: (I) -> O) = SafeFlange<I, O, Nothing> {
    for (e in rest()) {
        send(f(e))
    }
}

enum class XargsOnRemainder {
    Send,
    Ignore,
}

class XargsRemainderException(given: Int, expected: Int) :
    ArityException(given, listOf(expected), "xargsThrowOnRemainder($given, (..) => (..))", null)

fun <I, O> xargs(n: Int, f: (List<I>) -> O, onRemainder: XargsOnRemainder = XargsOnRemainder.Send) =
    SafeFlange<I, O, Nothing> {
        val list = mutableListOf<I>()
        for (e in rest()) {
            list.add(e)
            if (list.size == n) {
                send(f(list))
                list.clear()
            }
        }
        if (list.isNotEmpty()) when (onRemainder) {
            XargsOnRemainder.Send -> send(f(list))
            XargsOnRemainder.Ignore -> {}
        }
    }

fun <I, O> xargsThrowRemainder(n: Int, f: (List<I>) -> O) =
    UnsafeFlange<I, O, Nothing, XargsRemainderException> {
        val list = mutableListOf<I>()
        for (e in rest()) {
            list.add(e)
            if (list.size == n) {
                send(f(list))
                list.clear()
            }
        }
        if (list.isNotEmpty()) throw XargsRemainderException(list.size, n)
    }


fun <I, O> xargs2(f: (I, I) -> O) = xargs(2, { f(it[0], it[1]) }, XargsOnRemainder.Ignore)
fun <I, O> xargs3(f: (I, I, I) -> O) = xargs(3, { f(it[0], it[1], it[2]) }, XargsOnRemainder.Ignore)
fun <I, O> xargs4(f: (I, I, I, I) -> O) = xargs(4, { f(it[0], it[1], it[2], it[3]) }, XargsOnRemainder.Ignore)
fun <I, O> xargs5(f: (I, I, I, I, I) -> O) = xargs(5, { f(it[0], it[1], it[2], it[3], it[4]) }, XargsOnRemainder.Ignore)
fun <I, O> xargs6(f: (I, I, I, I, I, I) -> O) =
    xargs(6, { f(it[0], it[1], it[2], it[3], it[4], it[5]) }, XargsOnRemainder.Ignore)

fun <I, O> xargs7(f: (I, I, I, I, I, I, I) -> O) =
    xargs(7, { f(it[0], it[1], it[2], it[3], it[4], it[5], it[6]) }, XargsOnRemainder.Ignore)

fun <I, O> xargs8(f: (I, I, I, I, I, I, I, I) -> O) =
    xargs(8, { f(it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7]) }, XargsOnRemainder.Ignore)

fun <I, O> xargs9(f: (I, I, I, I, I, I, I, I, I) -> O) =
    xargs(9, { f(it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8]) }, XargsOnRemainder.Ignore)

fun <I, O> xargs10(f: (I, I, I, I, I, I, I, I, I, I) -> O) =
    xargs(10, { f(it[0], it[1], it[2], it[3], it[4], it[5], it[6], it[7], it[8], it[9]) }, XargsOnRemainder.Ignore)
