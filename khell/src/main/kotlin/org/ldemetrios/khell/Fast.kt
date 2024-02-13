package org.ldemetrios.khell

import org.ldemetrios.khell.sugar.Collect
import org.ldemetrios.khell.sugar.div
import java.io.File

operator fun String.invoke(): List<String> {
    val tempFile = File.createTempFile("script", ".scr")
    tempFile.writeText(this)
    tempFile.setExecutable(true)
   return terminal(tempFile.absolutePath) / Collect
}

operator fun String.get(vararg args: String): List<String> {
    return terminal(this, *args) / Collect
}

