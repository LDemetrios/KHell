@file:Suppress("UNUSED_PARAMETER")

package org.ldemetrios.khell.sugar

import kotlinx.coroutines.channels.SendChannel
import org.ldemetrios.khell.Flange
import org.ldemetrios.khell.PipelineContext
import org.ldemetrios.khell.SafeFlange
import org.ldemetrios.khell.Type

object Out

object Err

val `&1` = Out
val `&2` = Err
typealias Nil = Nothing?
typealias N = Nothing

private suspend fun <I, O, E, Ex : Throwable> call(
    flange: Flange<I, O, E, Ex>,
    parentContext: PipelineContext<I, *, *>,
    outInstead: SendChannel<O>?,
    errInstead:SendChannel<E>?,
) {
    flange.invoke(
        PipelineContext(
            parentContext.input,
            outInstead,
            errInstead,
                    parentContext.scope,
        )
    )
}

object doNothing : () -> Unit, (Any?) -> Unit, (Any?, Any?) -> Unit {
    override fun invoke() = Unit
    override fun invoke(p1: Any?) = Unit
    override fun invoke(p1: Any?, p2: Any?) = Unit
}

fun <I, OE, O : OE, E : OE, Ex : Throwable> redirect(outTo: Out, errTo: Out, flange: Flange<I, O, E, Ex>) =
    object : Flange<I, OE, Nothing, Ex> {
        override val type: Type<Ex> = flange.type
        override suspend fun invoke(pipelineContext: PipelineContext<I, OE, Nothing>) {
            call(
                flange,
                pipelineContext,
                pipelineContext.output,
                pipelineContext.output,
            )
        }
    }

fun <I, OE, O : OE, E : OE, Ex : Throwable> redirect(outTo: Out, errTo: Err, flange: Flange<I, O, E, Ex>) = flange

fun <I, OE, O : OE, E : OE, Ex : Throwable> redirect(outTo: Out, errTo: Nil, flange: Flange<I, O, E, Ex>) =
    object : Flange<I, O, Nothing, Ex> {
        override val type: Type<Ex> = flange.type

        override suspend fun invoke(pipelineContext: PipelineContext<I, O, Nothing>) {
            call(
                flange,
                pipelineContext,
                pipelineContext.output,
                null,
            )
        }
    }

fun <I, OE, O : OE, E : OE, Ex : Throwable> redirect(outTo: Err, errTo: Out, flange: Flange<I, O, E, Ex>) =
    object : Flange<I, E, O, Ex> {
        override val type: Type<Ex> = flange.type

        override suspend fun invoke(pipelineContext: PipelineContext<I, E, O>) {
            call(
                flange,
                pipelineContext,
                pipelineContext.error,
                pipelineContext.output,
            )
        }
    }

fun <I, OE, O : OE, E : OE, Ex : Throwable> redirect(outTo: Err, errTo: Err, flange: Flange<I, O, E, Ex>) =
    object : Flange<I, Nothing, OE, Ex> {
        override val type: Type<Ex> = flange.type

        override suspend fun invoke(pipelineContext: PipelineContext<I, Nothing, OE>) {
            call(
                flange,
                pipelineContext,
                pipelineContext.error,
                pipelineContext.error,
            )
        }
    }

fun <I, OE, O : OE, E : OE, Ex : Throwable> redirect(outTo: Err, errTo: Nil, flange: Flange<I, O, E, Ex>) =
    object : Flange<I, Nothing, O, Ex> {
        override val type: Type<Ex> = flange.type

        override suspend fun invoke(pipelineContext: PipelineContext<I, Nothing, O>) {
            call(
                flange,
                pipelineContext,
                pipelineContext.error,
                null,
            )
        }
    }

fun <I, OE, O : OE, E : OE, Ex : Throwable> redirect(outTo: Nil, errTo: Out, flange: Flange<I, O, E, Ex>) =
    object : Flange<I, E, Nothing, Ex> {
        override val type: Type<Ex> = flange.type
        override suspend fun invoke(pipelineContext: PipelineContext<I, E, Nothing>) {
            call(
                flange,
                pipelineContext,
                null,
                pipelineContext.output,
            )
        }
    }

fun <I, OE, O : OE, E : OE, Ex : Throwable> redirect(outTo: Nil, errTo: Err, flange: Flange<I, O, E, Ex>) =
    object : Flange<I, Nothing, E, Ex> {
        override val type: Type<Ex> = flange.type
        override suspend fun invoke(pipelineContext: PipelineContext<I, Nothing, E>) {
            call(
                flange,
                pipelineContext,
                null,
                pipelineContext.error,
            )
        }
    }

fun <I, OE, O : OE, E : OE, Ex : Throwable> redirect(outTo: Nil, errTo: Nil, flange: Flange<I, O, E, Ex>) =
    object : Flange<I, Nothing, Nothing, Ex> {
        override val type: Type<Ex> = flange.type
        override suspend fun invoke(pipelineContext: PipelineContext<I, Nothing, Nothing>) {
            call(
                flange,
                pipelineContext,
                null,
                null,
            )
        }
    }


//////////////////

fun <I, OE, O : OE, E : OE> redirect(outTo: Out, errTo: Out, flange: suspend (PipelineContext<I, O, E>).() -> Unit) =
    redirect(outTo, errTo, SafeFlange(flange))

fun <I, OE, O : OE, E : OE> redirect(outTo: Out, errTo: Err, flange: suspend (PipelineContext<I, O, E>).() -> Unit) =
    redirect(outTo, errTo, SafeFlange(flange))

fun <I, OE, O : OE, E : OE> redirect(outTo: Out, errTo: Nil, flange: suspend (PipelineContext<I, O, E>).() -> Unit) =
    redirect(outTo, errTo, SafeFlange(flange))

fun <I, OE, O : OE, E : OE> redirect(outTo: Err, errTo: Out, flange: suspend (PipelineContext<I, O, E>).() -> Unit) =
    redirect(outTo, errTo, SafeFlange(flange))

fun <I, OE, O : OE, E : OE> redirect(outTo: Err, errTo: Err, flange: suspend (PipelineContext<I, O, E>).() -> Unit) =
    redirect(outTo, errTo, SafeFlange(flange))

fun <I, OE, O : OE, E : OE> redirect(outTo: Err, errTo: Nil, flange: suspend (PipelineContext<I, O, E>).() -> Unit) =
    redirect(outTo, errTo, SafeFlange(flange))

fun <I, OE, O : OE, E : OE> redirect(outTo: Nil, errTo: Out, flange: suspend (PipelineContext<I, O, E>).() -> Unit) =
    redirect(outTo, errTo, SafeFlange(flange))

fun <I, OE, O : OE, E : OE> redirect(outTo: Nil, errTo: Err, flange: suspend (PipelineContext<I, O, E>).() -> Unit) =
    redirect(outTo, errTo, SafeFlange(flange))

fun <I, OE, O : OE, E : OE> redirect(outTo: Nil, errTo: Nil, flange: suspend (PipelineContext<I, O, E>).() -> Unit) =
    redirect(outTo, errTo, SafeFlange(flange))
