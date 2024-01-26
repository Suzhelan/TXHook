import toast "moe.ore.android.toast.Toast"
import CommonDialog "moe.ore.android.dialog.Dialog$CommonAlertBuilder"
import EditTextAlert "moe.ore.android.dialog.Dialog$EditTextAlertBuilder"
import "moe.ore.txhook.EntryActivity"

import "androidx.core.app.ActivityCompat"
import "android.content.pm.PackageManager"
import "android.content.pm.PackageManager"

import "java.lang.System"
import "java.lang.Thread"

import "moe.ore.android.util.OkhttpUtil"
import hex "moe.ore.txhook.helper.HexUtil"

import "java.security.KeyFactory"
import "java.security.spec.X509EncodedKeySpec"
import "javax.crypto.Cipher"
import "java.security.spec.PKCS8EncodedKeySpec"

import "moe.ore.android.util.DeviceIdUtils"

DEFAULT_PUBLIC_KEY = hex.Hex2Bin("30819F300D06092A864886F70D010101050003818D0030818902818100AA00F323F2DC4BBD0680CBC9FADF7AF5A8ED2E2962D44C38157B37260BD65EBFA6237258F2CC55E86B15007FA9D13E2701C3A88AD292FDD175858489D9587E7A3BA555956E9FC5F602A7A4F489C447782AC2647212B494A9EF4EC44DF82957FF405F7BB6C920D98C9D98645AFEF34FC003C707A1D2FF426CF7EAE0BDBD51D5FF0203010001")
DEFAULT_PRIVATE_KEY = hex.Hex2Bin("30820276020100300D06092A864886F70D0101010500048202603082025C02010002818100AA00F323F2DC4BBD0680CBC9FADF7AF5A8ED2E2962D44C38157B37260BD65EBFA6237258F2CC55E86B15007FA9D13E2701C3A88AD292FDD175858489D9587E7A3BA555956E9FC5F602A7A4F489C447782AC2647212B494A9EF4EC44DF82957FF405F7BB6C920D98C9D98645AFEF34FC003C707A1D2FF426CF7EAE0BDBD51D5FF020301000102818001CD86C68FD1C43FD9ECCDBC739BA11B2FD26C15E6456815842CCD55EAF43807024507F66784C13878C23D421D53E9BBD229F80498DD1431FF740E06C4364B090E5F288E700AF03A29B35BB43880786F2F2085E2FE28A700461075431D3ABF26BF241A4ED63CDD3A0B4B141C78DC150E392F16BA2A1EE3A823CB0B951ECAF409024100E05661CA5A2E474B160827851522D588CE0A9AC106268AEEEE0D5AB4A44432B1B57BEFCE44C6A0946B91B35088D1C1EAE1535D1BECB61FAB7E29B600CF9D6B7B024100C1FF6A8C9BDB1C483BFE5FC1270606DFAF55CD8DABCF8B6A502E23670D395B4498346AC78883ACA9EA2753473FC7B58053B1F2FE678E192B0664F13714B8E64D024059259A88A9DB78133B7714154B77E33910FF9FCD929F2058A01A886FFE52E77E3CEB3A39529547DC92FE7C2E45A06D19E45E974270875300780B253B1F45A41F0240480E2C6F297C8AD6B1A1DBC30C518AC00E89DA1D62D165C10922F9F74ECC1D002F6058C0E00DB8562C288B200DAA89D9AE3C8C3ABE0FE37D3D94C49B66D0FE890241009724365926B7E1063C41C552D5C9B45BCCA2197DBE27FBDC7D3D214387312243A31BCDC02125B2A7D83CD44FD995FDC25A7125B6C7FD732408C8CAE80C7CB220")

local fun decode(data) {
    keySpec = PKCS8EncodedKeySpec(DEFAULT_PRIVATE_KEY);
    keyFactory = KeyFactory.getInstance("RSA");
    privateKey = keyFactory.generatePrivate(keySpec);
    cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    cipher.init(Cipher.DECRYPT_MODE, privateKey);
    return cipher.doFinal(data);
}

local fun encode(data) {
    keyFactory = KeyFactory.getInstance("RSA")
    keySpec = X509EncodedKeySpec(DEFAULT_PUBLIC_KEY);
    publicKey = keyFactory.generatePublic(keySpec);
    cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
    return cipher.doFinal(data);
}

local fun exit(dialog, activity) {
    if(dialog ~= nil) {
        dialog.dismiss()
    }
    activity.finish()
    os.exit(1)
}

local fun enter(activity) {
    Thread.sleep(0.6 * 1000)
    print("欢迎使用TxHook")

    activity.gotoMain()
}

local fun checkAuth(activity) {
    val sha = activity.getSharedPreferences("auth", 0)

    val key = sha.getString("auth", nil)

    if(key == nil) {
                EditTextAlert(activity)
                .setTitle("捐赠验证")
                .setCancelable(false)
                .setFloatingText("请输入卡密")
                .setHint("输入卡密")
                .setTextListener({
                    onSubmit = fun(text) {
                        val edit = sha.edit()
                        edit.putString("auth", text)
                        edit.apply()
                        checkAuth(activity)
                    }
                })
                .setPositiveButton("确认", {
                    onClick = fun(dialog, which) {
                        dialog.dismiss()
                    }
                })
                .setNegativeButton("取消", {
                    onClick = fun(dialog, witch) {
                        exit(dialog, activity)
                    }
                })
                .show()
    } else {
        val imei = DeviceIdUtils.getDeviceId(activity)
        val http = OkhttpUtil()
        http.getSync("https://luololi.cn/txhook/?a=" .. hex.Bin2Hex(encode(key)) .. "&b=" .. hex.Bin2Hex(encode(imei)), {
            success = fun(call, resp) {
                source = decode(hex.Hex2Bin(resp.body.string()))
                if(source ~= "null") {

                    val edit = sha.edit()
                    edit.remove("auth")
                    edit.apply()

                    exit(nil, activity)
                } else {
                    enter(activity)
                }
            },
            failed = fun(call, ex) {
                exit(nil, activity)
            }
        })
    }
}

local fun comeToSetting(activity) {
    activity.gotoSetting()
}

local fun checkPer(activity) {
CommonDialog(activity)
                .setCancelable(false)
                .setTitle("给爷权限")
                .setMessage("爷要权限才能运行，所以说给爷！")
                .setPositiveButton("给", {
                    onClick = fun(dialog, witch) {
                        dialog.dismiss()
                        ActivityCompat.requestPermissions(activity,
                            EntryActivity.RequiredPermission,
                            EntryActivity.RequestPermissionCode);
                    }
               })
                .setNegativeButton("不给", {
                    onClick = fun(dialog, witch) {
                        exit(dialog, activity)
                    }
                 })
                .show()
}

return {
   onActivity = fun(activity) {
        if(activity.checkPermission() == false) {

        CommonDialog(activity)
                        .setCancelable(false)
                        .setTitle("使用警告")
                        .setMessage("该软件仅提供学习与交流使用，切勿应用于违法领域，并且请在24h内删除！")
                        .setPositiveButton("同意", {
                            onClick = fun(dialog, witch) {
                                dialog.dismiss()
                                checkPer(activity)
                            }
                       })
                        .setNegativeButton("不同意", {
                            onClick = fun(dialog, witch) {
                                exit(dialog, activity)
                            }
                         })
                        .show()
        } else {
            checkAuth(activity)
        }
    },
    onPermReqFail = fun(activity, code, permissions, grantResults) {
        CommonDialog(activity)
                .setCancelable(false)
                .setTitle("爷没有权限")
                .setMessage("爷要权限才能运行，但是你点了拒绝，现在立刻马上去设置给我打开权限")
                .setPositiveButton("去", {
                    onClick = fun(dialog, witch) {
                        dialog.dismiss()
                        comeToSetting(activity)
                    }
                })
                .setNegativeButton("不去", {
                    onClick = fun(dialog, witch) {
                        exit(dialog, activity)
                    }
                 })
                .show()
    },
    onPermReqSucc = fun(activity, code, permissions, grantResults) { checkAuth(activity) }
}


