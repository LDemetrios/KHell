package org.ldemetrios.khell

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.*
import org.ldemetrios.utilities.Either

interface Rinsable<in I, out O, out E, out Ex : Throwable> {
    fun rinseWith(list: List<I>): Triple<List<O>, List<E>, Ex?>
}

fun <O, E, Ex : Throwable> Rinsable<Nothing, O, E, Ex>.rinse() = rinseWith(listOf())

fun interface SuspendedIterable<out T> {
    operator fun iterator(): ChannelIterator<T>
}

data class PipelineContext<out I, in O, in E>(
    val input: ReceiveChannel<I>,
    val output: SendChannel<O>?,
    val error: SendChannel<E>?,
    val scope: CoroutineScope,
) : CoroutineScope by scope {
    suspend fun receive(): I = input.receive()

    suspend fun send(out: O) = output?.send(out) ?: Unit

    suspend fun sendError(err: E) = error?.send(err) ?: Unit

    suspend fun collect(): List<I> = input.toList()

    fun rest(): SuspendedIterable<I> = SuspendedIterable(input::iterator)

    suspend fun <Ex : Throwable> subshell(subshell: Rinsable<Nothing, O, E, Ex>) {
        val (out, err, ex) = subshell.rinse()

        for (line in err) {
            sendError(line)
        }
        for (line in out) {
            send(line)
        }

        if (ex != null) throw ex
    }

    suspend fun <Ex : Throwable> eval(subshell: Rinsable<Nothing, O, E, Ex>): List<@UnsafeVariance O> {
        val (out, err, ex) = subshell.rinse()

        for (line in err) {
            sendError(line)
        }

        if (ex != null) throw ex
        return out
    }

    suspend fun <Ex : Throwable> evalSafely(subshell: Rinsable<Nothing, O, E, Ex>): Either<List<@UnsafeVariance O>, Ex> {
        val (out, err, ex) = subshell.rinse()

        for (line in err) {
            sendError(line)
        }

        if (ex != null) return Either.Right(ex)
        return Either.Left(out)
    }
}

interface AqueductPart<in I, out O, out E, out Ex : Throwable> {
    val type: Type<@UnsafeVariance Ex>

    fun flanges(): List<Flange<*, *, E, Ex>>
}

interface Flange<in I, out O, out E, out Ex : Throwable> : Rinsable<I, O, E, Ex>, AqueductPart<I, O, E, Ex> {
    override val type: Type<@UnsafeVariance Ex>

    suspend operator fun invoke(pipelineContext: PipelineContext<I, O, E>): Unit

    override fun rinseWith(list: List<I>): Triple<List<O>, List<E>, Ex?> {
        return Aqueduct<I, O, _, Ex>(type, listOf(this)).rinseWith(list)
    }

    override fun flanges(): List<Flange<I, O, E, Ex>> = listOf(this)
}


inline fun <I, O, E, reified Ex : Throwable> UnsafeFlange(crossinline func: suspend PipelineContext<I, O, E>.() -> Unit) =
    object : Flange<I, O, E, Ex> {
        override val type = Type.of<Ex>()

        override suspend operator fun invoke(pipelineContext: PipelineContext<I, O, E>) = pipelineContext.func()
    }

inline fun <I, O, E> SafeFlange(crossinline func: suspend PipelineContext<I, O, E>.() -> Unit) =
    object : Flange<I, O, E, Nothing> {
        override val type: Type<Nothing> = Type.ofNothing()

        override suspend operator fun invoke(pipelineContext: PipelineContext<I, O, E>) = pipelineContext.func()
    }


inline fun <IO, E> barrier() = SafeFlange<IO, IO, E> {
    val input = collect()
    for (el in input) {
        send(el)
    }
}

fun <O, E, Ex : Throwable> shell(aqueduct: Rinsable<Nothing, O, E, Ex>): List<O> {
    val result = aqueduct.rinse()

    val (out, err, ex) = result

    for (line in err) {
        System.err.println(line)
    }

    if (ex != null) throw ex
    return out
}

fun <O, E, Ex : Throwable> safeShell(aqueduct: Rinsable<Nothing, O, E, Ex>): Either<List<@UnsafeVariance O>, Ex> {
    val (out, err, ex) = aqueduct.rinse()

    for (line in err) {
        System.err.println(line)
    }

    if (ex != null) return Either.Right(ex)
    return Either.Left(out)
}