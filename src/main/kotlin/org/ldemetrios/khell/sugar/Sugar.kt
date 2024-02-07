package org.ldemetrios.khell.sugar

import kotlinx.coroutines.CancellationException
import org.ldemetrios.khell.*
import org.ldemetrios.utilities.Either

//////////////////////////////////////////////////////////// Piping

operator fun <I, M, O, E, Ex : Throwable> AqueductPart<I, M, E, Ex>.div(that: AqueductPart<M, O, E, Ex>) =
    pipe(this, that)

operator fun <I, M, O, E, Ex : Throwable> (AqueductPart<I, M, E, Ex>).div(b: suspend PipelineContext<M, O, E>.() -> Unit) =
    pipe<I, M, O, E, Ex>(this, SafeFlange<M, O, E>(b))

//////////////////////////////////////////////////////////// Collecting

object Collect
object CollectSafe

operator fun <O, E, Ex : Throwable> Rinsable<Nothing, O, E, Ex>.div(collector: Collect): List<O> = shell(this)

operator fun <O, E, Ex : Throwable> Rinsable<Nothing, O, E, Ex>.div(collector: CollectSafe): Either<List<@UnsafeVariance O>, Ex> =
    safeShell(this)

//////////////////////////////////////////////////////////// Subshell sugar

suspend inline fun <I, O, E, reified Ex : Throwable> PipelineContext<I, O, E>.`$`(subshell: Rinsable<I, O, E, Ex>) =
    this.eval(subshell)

data class PipelineContextCat<I, O, E>(val context: PipelineContext<I, O, E>)

val <I, O, E> PipelineContext<I, O, E>.cat get() = PipelineContextCat(this)

suspend inline operator fun <I, O, E, reified Ex : Throwable> Rinsable<Nothing, O, E, Ex>.div(cat: PipelineContextCat<I, O, E>): Unit =
    cat.context.subshell(this)

//////////////////////////////////////////////////////////// Special functions
////////////////////////////// Detension basin

object Exclamation

typealias i = Exclamation
typealias `!` = Exclamation

operator fun <I, O, E, Ex : Throwable> (AqueductPart<I, O, E, Ex>).div(b: Exclamation) = this / barrier()

//////////////////////////////////////////////////////////// Other

suspend fun logCrash(str: String, func: suspend () -> Unit) {
    try {
        func()
        println("Noncrash $str")
    } catch (e: CancellationException) {
        println("Crash $str")
        throw e
    }
}

infix fun <T, R> T.`»`(func: (T) -> R) = func(this)
operator fun <T, R> T.minus(func: (T) -> R) = func(this)
