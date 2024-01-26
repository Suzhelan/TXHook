package moe.ore.txhook.helper

fun Thread.sleepQuietly(time: Long) {
    kotlin.runCatching {
        Thread.sleep(time)
    }
}