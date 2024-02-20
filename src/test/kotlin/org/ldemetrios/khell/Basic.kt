package org.ldemetrios.khell

import org.junit.jupiter.api.Test
import org.ldemetrios.khell.sugar.*

import org.junit.jupiter.api.Assertions.*

class Basic {
    @Test
    fun basic() {
        assertEquals(
            listOf(2, 4, 6, 8, 10),
            `$` {
                for (i in 1..10) send(i)
            } / {
                for (i in input) {
                    if (i % 2 == 0) send(i)
                }
            } / Collect
        )
    }
}