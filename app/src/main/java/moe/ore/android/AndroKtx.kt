package moe.ore.android

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.os.Environment.DIRECTORY_DOCUMENTS
import com.tencent.mmkv.MMKV
import com.tencent.mmkv.MMKVLogLevel
import moe.ore.android.toast.Toast
import moe.ore.txhook.BuildConfig
import java.io.File
import kotlin.properties.Delegates

@SuppressLint("StaticFieldLeak")
object AndroKtx {
    var isInit: Boolean = false
    var dataDir: String by Delegates.observable("") { _, _, new: String ->
        File(new).let { if (!it.exists()) it.mkdirs() }
    }
    var pDataDir: String by Delegates.observable("") { _, _, new: String ->
        File(new).let { if (!it.exists()) it.mkdirs() }
    }
    lateinit var context: Context

    fun init(context: Context) {
        this.dataDir =
            Environment.getExternalStoragePublicDirectory(DIRECTORY_DOCUMENTS).absolutePath + File.separator + "txhook"
        this.pDataDir = context.filesDir.absolutePath + File.separator + "txhook"

        MMKV.initialize(context, MMKVLogLevel.LevelNone) // 关闭mmkv日志
        this.context = context

        testRunning()
    }

    private fun testRunning() {
        kotlin.runCatching {
            val mmkv = MMKV.defaultMMKV()
            if (mmkv.decodeInt("version", 0) != BuildConfig.VERSION_CODE) {
                mmkv.removeValueForKey("version")
                mmkv.putInt("version", BuildConfig.VERSION_CODE)
            }
            mmkv.sync()
        }.onFailure {
            Toast.toast(msg = "遇到错误：" + it.message)
        }
    }
}