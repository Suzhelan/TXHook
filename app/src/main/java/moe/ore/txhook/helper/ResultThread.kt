package moe.ore.txhook.helper

import java.util.concurrent.BlockingQueue

/**
 * @author luoluo
 * @date 2020/10/2 0:48
 */
abstract class ResultThread<T> : Thread() {
    init {
        isDaemon = true
    }

    private var queue: BlockingQueue<T>? = null

    override fun run() {
        try {
            val t = on()
            queue?.add(t)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun setQueue(queue: BlockingQueue<T>) {
        this.queue = queue
    }

    @Throws(Throwable::class)
    abstract fun on(): T
}
