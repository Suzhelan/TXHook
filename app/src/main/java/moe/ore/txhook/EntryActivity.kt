package moe.ore.txhook

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
import androidx.core.app.ActivityCompat

import androidx.core.content.ContextCompat
import moe.ore.android.AndroKtx
import moe.ore.android.dialog.Dialog
import moe.ore.txhook.app.MainActivity
import moe.ore.xposed.helper.ConfigPusher
import kotlin.system.exitProcess

class EntryActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AndroKtx.isInit = true

        if (!checkPermission()) {
            Dialog.CommonAlertBuilder(this)
                .setCancelable(false)
                .setTitle("使用警告")
                .setMessage("该软件仅提供学习与交流使用，切勿应用于违法领域，并且请在24h内删除！")
                .setPositiveButton("同意") { dialog, _ ->
                    dialog.dismiss()
                    checkPer()
                }
                .setNegativeButton("不同意") { _, _ ->
                    exitProcess(1)
                }
                .show()
        } else gotoMain()
    }

    private fun checkPer() {
        Dialog.CommonAlertBuilder(this)
            .setCancelable(false)
            .setTitle("给爷权限")
            .setMessage("爷要权限才能运行，所以说给爷！")
            .setPositiveButton("给") { dialog, _ ->
                dialog.dismiss()
                ActivityCompat.requestPermissions(this, RequiredPermission, RequestPermissionCode)
            }
            .setNegativeButton("不给") { _, _ ->
                exitProcess(2)
            }
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == RequestPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ConfigPusher.initForOnce()

                gotoMain()
            } else {
                Dialog.CommonAlertBuilder(this)
                    .setCancelable(false)
                    .setTitle("爷没有权限")
                    .setMessage("爷要权限才能运行，但是你点了拒绝，现在立刻马上去设置给我打开权限")
                    .setPositiveButton("去") { dialog, _ ->
                        dialog.dismiss()
                        gotoSetting()
                    }
                    .setNegativeButton("不去") { _, _ ->
                        exitProcess(3)
                    }
                    .show()
            }
        }
    }

    private fun checkPermission(): Boolean {
        RequiredPermission.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun gotoMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun gotoSetting() {
        val intent = Intent(ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    companion object {
        private const val RequestPermissionCode = 203
        private val RequiredPermission = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
        )
    }
}

