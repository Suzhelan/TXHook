package moe.ore.xposed

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.MQQ
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQHD
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQLITE
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQMUSIC
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQSAFE
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.TIM
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.UNKNOWN
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.WEGAME

class HookEntry : IXposedHookLoadPackage {
    companion object {
        const val PACKAGE_NAME_QQ = "com.tencent.mobileqq"

        // const val PACKAGE_NAME_QQ_INTERNATIONAL = "com.tencent.mobileqqi"
        const val PACKAGE_NAME_QQ_LITE = "com.tencent.qqlite"
        const val PACKAGE_NAME_TIM = "com.tencent.tim"
        const val PACKAGE_NAME_QQHD = "com.tencent.minihd.qq"

        const val PACKAGE_NAME_WEGAME = "com.tencent.qt.qtl"
        const val PACKAGE_NAME_QM = "com.tencent.qqmusic"
        const val PACKAGE_NAME_QS = "com.tencent.token"

    }

    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        when (param.packageName) {
            PACKAGE_NAME_QQ -> StartupHook.doInit(MQQ, param.classLoader)
            PACKAGE_NAME_TIM -> StartupHook.doInit(TIM, param.classLoader)
            PACKAGE_NAME_QQ_LITE -> StartupHook.doInit(QQLITE, param.classLoader)
            PACKAGE_NAME_QQHD -> StartupHook.doInit(QQHD, param.classLoader)

            PACKAGE_NAME_QM -> StartupHook.doInit(QQMUSIC, param.classLoader)
            PACKAGE_NAME_WEGAME -> StartupHook.doInit(WEGAME, param.classLoader)
            PACKAGE_NAME_QS -> StartupHook.doInit(QQSAFE, param.classLoader)


            else -> StartupHook.doInit(UNKNOWN, param.classLoader)
        }
    }
}
