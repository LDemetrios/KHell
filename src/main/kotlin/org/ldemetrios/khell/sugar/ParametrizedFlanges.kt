package org.ldemetrios.khell.sugar

import org.ldemetrios.khell.*

//////////////////////////////////////////////////////////// Flange generators

open class FlangeParametrizer<E, Ex : Throwable>(val eType: Type<E>, val exType: Type<Ex>) {
    inline fun <reified NE> dirty(): FlangeParametrizer<NE, Ex> {
        require(eType.clazz == Nothing::class.java) {
            "Duplicate usage of .dirty<..>() on FlangeParametrizer: with ${eType.clazz}, then with ${NE::class.java}"
        }
        return FlangeParametrizer<NE, Ex>(Type.of<NE>(), exType)
    }

    inline fun <reified NEx : Throwable> unsafe(): FlangeParametrizer<E, NEx> {
        require(exType.clazz == Nothing::class.java) {
            "Duplicate usage of .unsafe<..>() on FlangeParametrizer: with ${exType.clazz}, then with ${NEx::class.java}"
        }
        return FlangeParametrizer<E, NEx>(eType, Type.of<NEx>())
    }

    inline fun <I, O, NE> dirtyAuto(crossinline func: suspend PipelineContext<I, O, NE>.() -> Unit): Flange<I, O, NE, Ex> =
        object : Flange<I, O, NE, Ex> {
            override val type: Type<Ex> = exType

            override suspend fun invoke(pipelineContext: PipelineContext<I, O, NE>) {
                pipelineContext.func()
            }
        }

    inline operator fun <I, O> invoke(crossinline func: suspend PipelineContext<I, O, E>.() -> Unit) =
        object : Flange<I, O, E, Ex> {
            override val type: Type<Ex> = exType

            override suspend fun invoke(pipelineContext: PipelineContext<I, O, E>) {
                pipelineContext.func()
            }
        }
}

open class NothingInFlangeParametrizer<E, Ex : Throwable>(val eType: Type<E>, val exType: Type<Ex>) {
    inline fun <reified NE> dirty(): NothingInFlangeParametrizer<NE, Ex> {
        require(eType.clazz == Nothing::class.java) {
            "Duplicate usage of .dirty<..>() on NothingInFlangeParametrizer: with ${eType.clazz}, then with ${NE::class.java}"
        }
        return NothingInFlangeParametrizer<NE, Ex>(Type.of<NE>(), exType)
    }

    inline fun <O, reified NE> dirtyAuto(crossinline func: suspend PipelineContext<Nothing, O, NE>.() -> Unit) =
        dirty<NE>()(func)

    inline fun <reified NEx : Throwable> unsafe(): NothingInFlangeParametrizer<E, NEx> {
        require(exType.clazz == Nothing::class.java) {
            "Duplicate usage of .unsafe<..>() on NothingInFlangeParametrizer: with ${exType.clazz}, then with ${NEx::class.java}"
        }
        return NothingInFlangeParametrizer<E, NEx>(eType, Type.of<NEx>())
    }

    inline operator fun <O> invoke(crossinline func: suspend PipelineContext<Nothing, O, E>.() -> Unit) =
        object : Flange<Nothing, O, E, Ex> {
            override val type: Type<Ex> = exType

            override suspend fun invoke(pipelineContext: PipelineContext<Nothing, O, E>) {
                pipelineContext.func()
            }
        }
}

inline fun <reified NE> dirty(): FlangeParametrizer<NE, Nothing> = FlangeParametrizer(Type.of<NE>(), Type.ofNothing())
inline fun <I, O, NE> dirtyAuto(crossinline func: suspend PipelineContext<I, O, NE>.() -> Unit) =
    object : Flange<I, O, NE, Nothing> {
        override val type: Type<Nothing> = Type.ofNothing()

        override suspend fun invoke(pipelineContext: PipelineContext<I, O, NE>) {
            pipelineContext.func()
        }
    }

inline fun <reified NEx : Throwable> unsafe(): FlangeParametrizer<Nothing, NEx> =
    FlangeParametrizer(Type.ofNothing(), Type.of<NEx>())

/////////////////////////////////////// `$` syntax
object Dollar : NothingInFlangeParametrizer<Nothing, Nothing>(Type.ofNothing(), Type.ofNothing()) {
//    // Nothing, O, Nothing, Nothing
//    override operator fun <O> invoke(func: suspend PipelineContext<Nothing, O, Nothing>.() -> Unit) =
//        SafeFlange<Nothing, O, Nothing>(func)
}

typealias S = Dollar
typealias `$` = Dollar

////////////////////////////////////// Beware & expect (?)

class Beware<Ex : Throwable>(val type: Type<Ex>)

inline fun <reified Ex : Throwable> beware() = Beware(Type.of<Ex>())

operator fun <I, O, E, Ex : Throwable, Ex1 : Ex, Ex2 : Ex> AqueductPart<I, O, E, Ex1>.div(beware: Beware<Ex2>) =
    Aqueduct<I, O, E, Ex>(Type.common(this.type, beware.type), flanges())
