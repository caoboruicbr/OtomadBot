@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package xyz.xszq.otomadbot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import xyz.xszq.otomadbot.kotlin.newTempFile
import java.io.File

data class Argument(val key: String, val value: String = "") {
    override fun toString(): String {
        var result = "-$key"
        if (value.isNotBlank())
            result += " $value"
        return result
    }
    fun toTypedArray(): Array<String> {
        val result = mutableListOf("-$key")
        if (value.isNotBlank())
            result.add(value)
        return result.toTypedArray()
    }
}
data class FFMpegFileType(val ext: String, val requiredArgs: List<Argument> = emptyList()) {
    companion object {
        val MP3 = FFMpegFileType("mp3")
        val WAV = FFMpegFileType("wav")
        val PCM = FFMpegFileType("pcm")
    }
}
class FFMpegTask(val outputFormat: FFMpegFileType,
                 val argsBuilder: Builder.() -> Unit) {
    @DslMarker annotation class FFMpegBuilder
    @FFMpegBuilder
    inner class Builder {
        val arguments = mutableListOf<Argument>()
        private fun insert(arg: Argument) = arguments.add(arg)
        fun input(path: String) = insert(Argument("i", path))
        fun input(path: File) = insert(Argument("i", path.absolutePath))
        fun startAt(timeInSecond: Double) = insert(Argument("ss", timeInSecond.toString()))
        fun duration(timeInSecond: Double) = insert(Argument("t", timeInSecond.toString()))
        fun audioRate(rate: String) = insert(Argument("ar", rate))
        fun audioChannels(channels: Int) = insert(Argument("ac", channels.toString()))
        fun yes() = insert(Argument("y"))
        fun audioFilter(filter: String) = insert(Argument("af", filter))
        fun acodec(type: String) = insert(Argument("acodec", type))
        fun forceFormat(format: String) = insert(Argument("f", format))
    }
    var ffmpegPath: String = BinConfig.data.values["ffmpeg"] ?: "ffmpeg"
    var ffmpegEnv = BinConfig.data.values["ffmpegPath"] ?: ""
    fun buildCommand(): String {
        var result = ffmpegPath
        Builder().apply(argsBuilder).arguments.forEach { result += " $it" }
        return result
    }
    private fun getOutputFile(): File = newTempFile(suffix=".${outputFormat.ext}")
    private fun getResultBlocking(): File? {
        checkFFMpeg(ffmpegPath)
        val result = getOutputFile()
        var command = buildCommand()
        command += " ${result.absolutePath}"
        println(command)
        return try {
            runBlocking {
                ProgramExecutor(command) {
                    environment {
                        append(ffmpegEnv)
                    }
                }.start()
            }
            result
        } catch (e: Exception) {
            null
        }
    }
    suspend fun getResult(): File? = withContext(Dispatchers.IO) {
        getResultBlocking()
    }
    companion object {
        fun checkFFMpeg(path: String = BinConfig.data.values["ffmpeg"] ?: "ffmpeg") {
            if (!File(path).exists())
                println("Warn: FFMpeg does not exist")
        }
    }
}