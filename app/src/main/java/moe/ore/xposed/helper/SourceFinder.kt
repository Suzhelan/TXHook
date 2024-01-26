package moe.ore.xposed.helper

import moe.ore.android.AndroKtx
import java.io.File

object SourceFinder {
    private var init: Boolean = false

    private inline fun init() {
        if (init) return
        val f = File(AndroKtx.dataDir + "/.package")
        if (!f.exists())
            f.mkdirs()
        else init = true
    }

    fun pushPb(hash: Int, name: String) {
        init()
        val allFile = File(AndroKtx.dataDir + "/.package/" + hash)
        if (!allFile.exists())
            allFile.createNewFile()
        allFile.writeText(name)
    }

    fun find(hash: Int): String? {
        init()
        val allFile = File(AndroKtx.dataDir + "/.package/" + hash)
        if (allFile.exists()) {
            return allFile.readText()
        }
        return null
    }

    fun clear() {
        val f = File(AndroKtx.dataDir + "/.package")
        f.deleteRecursively()
    }
}