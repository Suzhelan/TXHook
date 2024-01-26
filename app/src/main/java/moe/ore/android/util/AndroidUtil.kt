package moe.ore.android.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.ContextCompat
import moe.ore.android.toast.Toast

object AndroidUtil {
    fun dip2px(context: Context, dpValue: Float): Int {
        val scale: Float = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    @JvmStatic
    fun copyText(context: Context?, text: CharSequence) {
        val cm = ContextCompat.getSystemService(context!!, ClipboardManager::class.java)
        val mClipData = ClipData.newPlainText("Label", text)
        cm?.setPrimaryClip(mClipData)
        Toast.toast(context, "复制成功")
    }
}