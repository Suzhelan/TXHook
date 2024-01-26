package moe.ore.xposed.helper

import moe.ore.android.AndroKtx
import moe.ore.txhook.helper.closeQuietly
import java.io.File
import java.io.RandomAccessFile

object DataPutter {
    fun put(kind: DataKind, string: String) {
        val allFile = File(AndroKtx.dataDir + "/" + kind.dir)
        if (allFile.exists()) {
            allFile.appendText("\n" + string)
        } else {
            allFile.writeText(string)
        }
    }

    fun put(kind: DataKind, bytes: ByteArray) {
        val allFile = File(AndroKtx.dataDir + "/" + kind.dir)
        RandomAccessFile(allFile, "rw").also {
            it.seek(it.length())
            it.write(bytes)
        }.closeQuietly()
    }

    fun getRF(kind: DataKind): RandomAccessFile {
        val allFile = File(AndroKtx.dataDir + "/" + kind.dir)
        return RandomAccessFile(allFile, "rw")
    }

    fun clear(kind: DataKind) {
        File(AndroKtx.dataDir + "/" + kind.dir).deleteOnExit()
    }
}

enum class DataKind(val dir: String) {
    QLOG("qlog.log"),
    WTLOGIN_LOG("wlogin_sdk.log"),
    ECDH_PUBLIC("全部公钥.txt"),
    ECDH_SHARE("全部私钥.txt"),
    MATCH_PACKAGE("match.txt"),
}