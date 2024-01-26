package moe.ore.txhook.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.leon.lib.settingview.LSettingItem
import com.tencent.mmkv.MMKV
import moe.ore.android.AndroKtx
import moe.ore.android.dialog.Dialog
import moe.ore.android.toast.Toast
import moe.ore.android.util.FuckSettingItem
import moe.ore.script.Consist
import moe.ore.txhook.databinding.FragmentSettingBinding
import moe.ore.xposed.helper.ConfigPusher
import moe.ore.xposed.helper.ConfigPusher.KEY_ECDH_DEFAULT
import moe.ore.xposed.helper.ConfigPusher.KEY_ECDH_NEW_HOOK
import moe.ore.xposed.helper.ConfigPusher.KEY_FORBID_TCP
import moe.ore.xposed.helper.ConfigPusher.KEY_OPEN_LOG
import moe.ore.xposed.helper.ConfigPusher.KEY_PUSH_API
import moe.ore.xposed.helper.ConfigPusher.KEY_WS_ADDRESS
import moe.ore.xposed.helper.DataKind
import moe.ore.xposed.helper.DataPutter
import moe.ore.xposed.helper.SourceFinder

class SettingFragment : Fragment() {
    private lateinit var binding: FragmentSettingBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentSettingBinding.inflate(inflater, container, false).also {
        this.binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val addressText = binding.address
        addressText.text = ConfigPusher[KEY_PUSH_API].orEmpty().ifBlank { "未配置地址" }

        binding.merge.let {
            it.setmOnLSettingItemClick(SwitchSettingListenerByMMKV(it, Consist.MMKV_HANDLE_MERGE))
        }

        binding.autoMerge.let {
            it.setmOnLSettingItemClick(
                SwitchSettingListenerByMMKV(
                    it,
                    Consist.MMKV_MERGE_PROTOBUF,
                    true
                )
            )
        }

        binding.switchNHook.let {
            it.setmOnLSettingItemClick(object :
                SwitchSettingListener(it, ConfigPusher[KEY_ECDH_NEW_HOOK] != "no", true) {
                override fun onClick(isChecked: Boolean) {
                    ConfigPusher[KEY_ECDH_NEW_HOOK] = if (isChecked) "yes" else "no"
                }
            })
        }

        FuckSettingItem.setSwitchListener(binding.pushApi.also {
            if (ConfigPusher[KEY_PUSH_API].orEmpty().isNotEmpty())
                FuckSettingItem.turnSettingSwitch(it, true)
        }) {
            if ((it as SwitchCompat).isChecked) {
                Dialog.EditTextAlertBuilder(requireContext())
                    .setTitle("输入目标地址")
                    .setTextListener { text ->
                        if (text.isNullOrBlank()) {
                            FuckSettingItem.turnSettingSwitch(binding.pushApi, false)
                            return@setTextListener
                        }
                        ConfigPusher[KEY_PUSH_API] = text.toString()
                        addressText.text = text
                        Toast.toast(requireContext(), "Push服务配置成功")
                        FuckSettingItem.turnSettingSwitch(binding.pushApi, true)
                    }
                    .setFloatingText("请输入你自己的Domain：")
                    .setHint("blog.xinrao.co")
                    .setPositiveButton("确定") { dialog, _ ->
                        dialog.dismiss()
                    }.setOnCancelListener {
                        FuckSettingItem.turnSettingSwitch(binding.pushApi, false)
                    }
                    .show()
            } else {
                addressText.text = "未配置服务"
                ConfigPusher[KEY_PUSH_API] = ""
                Toast.toast(requireContext(), "Push服务已关闭")
            }
        }

        FuckSettingItem.setSwitchListener(binding.wsApi.also {
            if (ConfigPusher[KEY_WS_ADDRESS].orEmpty().isNotEmpty())
                FuckSettingItem.turnSettingSwitch(it, true)
        }) {
            if ((it as SwitchCompat).isChecked) {
                Dialog.EditTextAlertBuilder(requireContext())
                    .setTitle("输入目标地址")
                    .setTextListener { text ->
                        if (text.isNullOrBlank()) {
                            FuckSettingItem.turnSettingSwitch(binding.wsApi, false)
                            return@setTextListener
                        }
                        ConfigPusher[KEY_WS_ADDRESS] = text.toString()
                        Toast.toast(requireContext(), "WS服务配置成功")
                        FuckSettingItem.turnSettingSwitch(binding.wsApi, true)
                    }
                    .setFloatingText("请输入你自己的Domain：")
                    .setHint("wss://xx.xx.xx/ws")
                    .setPositiveButton("确定") { dialog, _ ->
                        dialog.dismiss()
                    }.setOnCancelListener {
                        FuckSettingItem.turnSettingSwitch(binding.wsApi, false)
                    }
                    .show()
            } else {
                ConfigPusher[KEY_WS_ADDRESS] = ""
                Toast.toast(requireContext(), "WS服务已关闭")
            }
        }

        binding.outQlog.also {
            if (ConfigPusher[KEY_OPEN_LOG] == "yes") it.clickOn()
        }.setmOnLSettingItemClick {
            ConfigPusher[KEY_OPEN_LOG] = if (it) "yes" else "no"
            Toast.toast(msg = "需要重新启动QQ后生效")
            Toast.toast(msg = "日志生成在：${AndroKtx.dataDir}")
        }

        binding.ecdhDef.also {
            if (ConfigPusher[KEY_ECDH_DEFAULT] == "yes")
                it.clickOn()
        }.setmOnLSettingItemClick {
            ConfigPusher[KEY_ECDH_DEFAULT] = if (it) "yes" else "no"
            Toast.toast(msg = "需要重新启动QQ后生效")
        }

        binding.forbidTcp.let {
            it.setmOnLSettingItemClick(object :
                SwitchSettingListener(it, ConfigPusher[KEY_FORBID_TCP] == "yes", true) {
                override fun onClick(isChecked: Boolean) {
                    ConfigPusher[KEY_FORBID_TCP] = if (isChecked) "yes" else "no"
                }
            })
        }

        binding.claerCache.let {
            it.setmOnLSettingItemClick {
                DataPutter.clear(DataKind.ECDH_PUBLIC)
                DataPutter.clear(DataKind.ECDH_SHARE)
                DataPutter.clear(DataKind.QLOG)
                DataPutter.clear(DataKind.WTLOGIN_LOG)
                DataPutter.clear(DataKind.MATCH_PACKAGE)
                SourceFinder.clear()
                Toast.toast(msg = "清理成功")
            }
        }
    }

    class SwitchSettingListenerByMMKV(
        view: LSettingItem,
        private val key: String, def: Boolean = false,
        private val needRe: Boolean = false
    ) : SwitchSettingListener(view, MMKV.defaultMMKV().decodeBool(key, def), needRe) {
        override fun onClick(isChecked: Boolean) {
            MMKV.defaultMMKV().also {
                it.encode(key, isChecked)
            }.async()
        }
    }

    abstract class SwitchSettingListener(
        view: LSettingItem,
        value: Boolean = false,
        private val needRe: Boolean = false
    ) : LSettingItem.OnLSettingItemClick {
        init {
            if (value) view.clickOn()
        }

        abstract fun onClick(isChecked: Boolean)

        override fun click(isChecked: Boolean) {
            this.onClick(isChecked)
            if (needRe) Toast.toast(msg = "需要重新启动QQ后生效")
        }
    }
}