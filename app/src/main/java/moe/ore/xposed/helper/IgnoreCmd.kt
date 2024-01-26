package moe.ore.xposed.helper

import moe.ore.android.AndroKtx
import moe.ore.txhook.helper.FileUtil
import java.io.File

object IgnoreCmd {
    fun isIgnore(cmd: String): Boolean {
        val dir = AndroKtx.dataDir + "/ignore.txt"
        if (FileUtil.has(dir)) {
            File(dir).readLines().forEach {
                if (it.contentEquals(cmd)) return true
            }
        }
        return false
    }

    fun appendIgnore(cmd: String): Boolean {
        val allFile = File(AndroKtx.dataDir + "/ignore.txt")
        if (allFile.exists()) {
            if (!isIgnore(cmd))
                allFile.appendText("\n" + cmd)
        } else {
            allFile.writeText(cmd)
        }
        return false
    }
}