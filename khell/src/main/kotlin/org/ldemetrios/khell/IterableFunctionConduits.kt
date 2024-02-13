@file:Suppress("unused")

package org.ldemetrios.khell

import kotlinx.coroutines.channels.toList

fun <T> taking(n: Int) = if (n >= 0) SafeFlange<T, T, Nothing> {
    var lim = 0
    for (e in input) {
        if (lim++ >= n) break
        send(e)
    }
} else SafeFlange<T, T, Nothing> {
    @Suppress("NAME_SHADOWING") val n = -n
    // Anything but last n
    val queue = Array<Any?>(n) { null }
    for (i in 0 until n) {
        queue[i] = receive()
    }
    var i = 0
    for (e in input) {
        @Suppress("UNCHECKED_CAST")
        send(queue[i] as T)
        queue[i] = receive()
        i++
        if (i == n) i = 0
    }
}

fun <T> dropping(n: Int) = if (n >= 0) SafeFlange<T, T, Nothing> {
    repeat(n) { receive() }
    for (e in input) send(receive())
} else SafeFlange<T, T, Nothing> {
    @Suppress("NAME_SHADOWING") val n = -n
    // Anything but last n
    var i = 0
    val queue = Array<Any?>(n) { null }
    for (e in input) {
        queue[i] = receive()
        i++
        if (i == n) i = 0
    }
    repeat(n) {
        @Suppress("UNCHECKED_CAST")
        send(queue[i] as T)
        i++
        if (i == n) i = 0
    }
}

fun <T> droppingWhile(predicate: (T) -> Boolean) = SafeFlange<T, T, Nothing> {
    var e: T
    do {
        e = receive()
    } while (predicate(e))
    send(e)
    for (@Suppress("NAME_SHADOWING") e in input) send(receive())
}

fun <T> droppingLastWhile(predicate: (T) -> Boolean) = SafeFlange<T, T, Nothing> {
    val queue = mutableListOf<T>()
    for (e in input) {
        if (predicate(e)) queue.add(e)
        else {
            for (prev in queue) send(prev)
            send(e)
            queue.clear()
        }
    }
}

fun <T> filtering(predicate: (T) -> Boolean) = SafeFlange<T, T, Nothing> {
    for (e in input) {
        if (predicate(e)) send(e)
    }
}

fun <T> filteringIndexed(predicate: (index: Int, T) -> Boolean) = SafeFlange<T, T, Nothing>{
    var ind = 0
    for (e in input) {
        if (predicate(ind, e)) send(e)
        ind++
    }
}

fun <T> distinct() = SafeFlange<T, T, Nothing> {
    val set = mutableSetOf<T>()
    for (e in input) {
        if (e !in set) {
            send(e)
            set.add(e)
        }
    }
}

fun <T, R> distinctBy(func: (T) -> R) = SafeFlange<T, T, Nothing> {
    val set = mutableSetOf<R>()
    for (e in input) {
        val f = func(e)
        if (f !in set) {
            send(e)
            set.add(f)
        }
    }
}

inline fun <T, reified R> filteringIsInstance() = SafeFlange<T, T, Nothing> {
    for (e in input) {
        if (e is R) send(e)
    }
}

fun <T> takingWhile(predicate: (T) -> Boolean) = SafeFlange<T, T, Nothing> {
    for (e in input) {
        if (predicate(e)) send(e)
        else break
    }
}

fun <T> takingLastWhile(predicate: (T) -> Boolean) = SafeFlange<T, T, Nothing> {
    val queue = mutableListOf<T>()
    for (e in input) {
        if (predicate(e)) queue.add(e)
        else {
            queue.clear()
        }
    }
    for (e in queue) send(e)
}

fun <T, R> applying(func: (List<T>) -> Iterable<R>) = SafeFlange<T, R, Nothing> {
    val list = input.toList()
    for (e in func(list)) send(e)
}

fun <T, R> mapping(func: (T) -> R) = SafeFlange<T, R, Nothing>{
    for (e in input) send(func(receive()))
}

fun <T, R> flatMapping(func: (T) -> Iterable<R>) = SafeFlange<T, R, Nothing> {
    for (e in input) for (out in func(e)) send(out)
}

fun <T, R> flatMappingSeq(func: (T) -> Sequence<R>) = SafeFlange<T, R, Nothing> {
    for (e in input) for (out in func(e)) send(out)
}

fun <T> indexing() = SafeFlange<T, IndexedValue<T>, Nothing> {
    var ind = 0
    for (e in input) {
        send(IndexedValue(ind, e))
        ind++
    }
}

fun <T> counting() = SafeFlange<T, Long,  Nothing> {
    var count = 0L
    for (e in input) count++
    send(count)
}

fun <T> checkingAll(predicate: (T) -> Boolean) = SafeFlange<T, Boolean,  Nothing> {
    for (e in input) if (!predicate(e)) {
        send(false)
        return@SafeFlange
    }
    send(true)
}

fun <T> checkingAny(predicate: (T) -> Boolean) = SafeFlange<T, Boolean,  Nothing>{
    for (e in input) if (predicate(e)) {
        send(true)
        return@SafeFlange
    }
    send(false)
}

fun <T> checkingNone(predicate: (T) -> Boolean) = SafeFlange<T, Boolean,  Nothing>{
    for (e in input) if (predicate(e)) {
        send(false)
        return@SafeFlange
    }
    send(true)
}

fun <T, R> folding(initial: R, operation: (acc: R, T) -> R) = SafeFlange<T, R,  Nothing> {
    var state = initial
    for (e in input) state = operation(state, e)
    send(state)
}

fun <T, R> foldingIndexed(initial: R, operation: (index: Int, acc: R, T) -> R) = SafeFlange<T, R,  Nothing> {
    var state = initial
    var ind = 0
    for (e in input) {
        state = operation(ind, state, e)
        ind++
    }
    send(state)
}

fun <T, R> foldingRight(initial: R, operation: (T, acc: R) -> R) = SafeFlange<T, R,  Nothing> {
    var state = initial
    for (e in input) state = operation(e, state)
    send(state)
}

fun <T, R> foldingIndexedRight(initial: R, operation: (index: Int, T, acc: R) -> R) = SafeFlange<T, R,  Nothing> {
    var state = initial
    var ind = 0
    for (e in input) {
        state = operation(ind, e, state)
        ind++
    }
    send(state)
}

fun <S, T : S> reducing(operation: (acc: S, T) -> S) = SafeFlange<T, S,  Nothing> {
    var state: S = receive()
    for (e in input) state = operation(state, e)
    send(state)
}

fun <S, T : S> reducingIndexed(operation: (index: Int, acc: S, T) -> S) = SafeFlange<T, S,  Nothing>{
    var state: S = receive()
    var ind = 0
    for (e in input) {
        state = operation(ind, state, e)
        ind++
    }
    send(state)
}

fun <S, T : S> reducingRight(operation: (T, acc: S) -> S) = SafeFlange<T, S,  Nothing>{
    var state: S = receive()
    for (e in input) state = operation(e, state)
    send(state)
}

fun <S, T : S> reducingIndexedRight(operation: (index: Int, T, acc: S) -> S) = SafeFlange<T, S,  Nothing>{
    var state: S = receive()
    var ind = 0
    for (e in input) {
        state = operation(ind, e, state)
        ind++
    }
    send(state)
}

fun <T, R> runningFolding(initial: R, operation: (acc: R, T) -> R) = SafeFlange<T, R,  Nothing> {
    var state = initial
    for (e in input) {
        state = operation(state, e)
        send(state)
    }
}

fun <T, R> runningFoldingIndexed(initial: R, operation: (index: Int, acc: R, T) -> R) = SafeFlange<T, R,  Nothing> {
    var state = initial
    var ind = 0
    for (e in input) {
        state = operation(ind, state, e)
        ind++
        send(state)
    }
}

fun <S, T : S> runningReducing(operation: (acc: S, T) -> S) = SafeFlange<T, S,  Nothing> {
    var state: S = receive()
    send(state)
    for (e in input) {
        state = operation(state, e)
        send(state)
    }
}

fun <S, T : S> runningReducingIndexed(operation: (index: Int, acc: S, T) -> S) = SafeFlange<T, S,  Nothing>{
    var state: S = receive()
    var ind = 0
    send(state)
    for (e in input) {
        state = operation(ind, state, e)
        ind++
        send(state)
    }
}

fun <S, T : S> runningReducingRight(operation: (T, acc: S) -> S) = SafeFlange<T, S,  Nothing> {
    var state: S = receive()
    send(state)
    for (e in input) {
        state = operation(e, state)
        send(state)
    }
}


fun <S, T : S> runningReducingIndexedRight(operation: (index: Int, T, acc: S) -> S) = SafeFlange<T, S,  Nothing>{
    var state: S = receive()
    var ind = 0
    send(state)
    for (e in input) {
        state = operation(ind, e, state)
        ind++
        send(state)
    }
}

fun <T> cat(list: Iterable<T>)  = SafeFlange<Nothing, T,  Nothing> {
    for (el in list) send(el)
}

fun <T> cat(list: Iterator<T>)  = SafeFlange<Nothing, T,  Nothing> {
    for (el in list) send(el)
}

fun <T> iterate(initial: T, next: (T) -> T)  = SafeFlange<Nothing, T,  Nothing> {
    var state: T = initial
    while (true) {
        send(state)
        state = next(state)
    }
}

fun <T> repeat(supplier: () -> T)  = SafeFlange<Nothing, T,  Nothing> {
    while (true) send(supplier())
}
