package org.ldemetrios.khell

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.toList

inline fun <I, O, E, reified Ex : Throwable> AutoAqueduct(flanges: List<Flange<*, *, E, Ex>>) =
    Aqueduct<I, O, E, Ex>(Type.of<Ex>(), flanges)

data class Aqueduct<in I, out O, out E, out Ex : Throwable> @PublishedApi internal constructor(
    override val type: Type<@UnsafeVariance Ex>,
    val flanges: List<Flange<*, *, E, Ex>>,
) : Rinsable<I, O, E, Ex>, AqueductPart<I, O, E, Ex> {
    override fun rinseWith(list: List<I>): Triple<List<O>, List<E>, Ex?> {
        return runBlocking {
            flush(
                this,
                type,
                list,
                this@Aqueduct
            )
        }
    }

    override fun flanges(): List<Flange<*, *, E, Ex>> = flanges
}

internal suspend fun <I, O, E, Ex : Throwable> flush(
    scope: CoroutineScope,
    clazz: Type<out Throwable>,
    inputData: List<I>,
    aqueduct: Aqueduct<I, O, E, Ex>
): Triple<List<O>, List<E>, Ex?> {
    val flanges: List<Flange<*, *, E, Ex>> = aqueduct.flanges
    val input = Channel<Any?>(inputData.size)
    for (el in inputData) input.send(el)
    input.close()
    val pipes = List(flanges.size + 1) { if (it == 0) input else Channel() }

    val error = Channel<E>(Channel.UNLIMITED)
    var except: Ex? = null

    var jobs: List<Job>? = null

    val cancelAll: () -> Unit = {
        jobs?.forEach(Job::cancel)
        pipes.forEach(Channel<*>::close)
    }

    jobs = flanges.mapIndexed { i, flange ->
        scope.launch {
            val result = try {
                (flange as Flange<Any?, Any?, E, Ex>)(PipelineContext(pipes[i], pipes[i + 1], error, this))
                null
            } catch (e: Exception) {
                cancelAll()
                if (clazz.checkIs(e)) {
//                    System.err.println("Right: $clazz -- $e")
//                    e.printStackTrace()
                    e as Ex
                } else {
//                    System.err.println("Left: not $clazz -- $e")
//                    e.printStackTrace()
                    throw e
                }
            }
//            println("out of $flange")
            pipes[i + 1].close()
            if (result != null) {
                except = result
                cancelAll()
            }
            if (i == flanges.lastIndex) {
                cancelAll()
            }
//            println("$flange's last word")
        }
    }

//    println("main: main")
    val lastOut = pipes.last().toList()
//    println("main: $lastOut")
    error.close()
    val errorOut = error.toList()
//    println("--- ErrorList: $errorOut")
    return Triple(
        lastOut.map { it as O },
        errorOut,
        except
    )
}

inline fun <I, O, E, reified Ex : Throwable> AqueductOf(flange: Flange<I, O, E, Ex>) =
    Aqueduct<I, O, E, Ex>(flange.type, listOf(flange))

fun <I, M, O, E, Ex : Throwable> pipe(a: AqueductPart<I, M, E, Ex>, b: AqueductPart<M, O, E, Ex>) =
    Aqueduct<I, O, E, Ex>(Type.common(a.type, b.type), a.flanges() + b.flanges())

