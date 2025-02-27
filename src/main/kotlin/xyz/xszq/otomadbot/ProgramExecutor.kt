@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package xyz.xszq.otomadbot

import com.soywiz.korio.util.OS.isLinux
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value
import xyz.xszq.OtomadBotCore
import java.util.concurrent.TimeUnit

@DslMarker annotation class ExecBuilder
@DslMarker annotation class EnvBuilder
class ProgramExecutor(val command: String, val builder: Builder.() -> Unit = {}) {
    @ExecBuilder
    inner class Builder {
        var env = emptyArray<String>()
        var timeout: Long? = null
        fun environment(builder: EnvironmentBuilder.() -> Unit) {
            env = EnvironmentBuilder().apply(builder).env.toTypedArray()
        }
        fun timeout(timeMs: Long) { timeout = timeMs }
    }
    @EnvBuilder
    inner class EnvironmentBuilder {
        val env = mutableListOf<String>()
        fun append(str: String) = if (str.isNotBlank()) env.add(str) else false
        fun append(str: String?) = str?.let { if (it.isNotBlank()) env.add(it) }
    }
    private fun startBlocking(): Unit = Builder().apply(builder).run {
        val procBuilder = ProcessBuilder()
        env.forEach {
            val args = it.split("=")
            val name = args.first()
            val value = args.last()
            procBuilder.environment().putIfAbsent(name, value)
        }
        val realCommand = if (isLinux)
            listOf("/bin/bash", "-c", command)
        else
            listOf("C:\\Windows\\System32\\cmd.exe", "/C", command)
        val proc = procBuilder.inheritIO().command(realCommand).start()
        timeout?.let {
            proc.waitFor(it, TimeUnit.MILLISECONDS)
        } ?: run {
            proc.waitFor()
        }
    }
    suspend fun start() = withContext(Dispatchers.IO) {
        startBlocking()
    }
}

object BinConfig: SafeYamlConfig<MapStringValues>(OtomadBotCore, "bin", MapStringValues(buildMap {
    put("ffmpeg", "ffmpeg")
    put("ffmpegPath", "")
    put("python", "/usr/bin/python3")
    put("pitch_shift", "PitchCorrection4Mirai.py") }.toMutableMap()))