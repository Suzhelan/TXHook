package moe.ore.android.util

import android.view.View
import androidx.appcompat.widget.SwitchCompat
import com.leon.lib.settingview.LSettingItem
import java.lang.reflect.Field

object FuckSettingItem {
    private lateinit var switchField: Field
    private lateinit var checkOnField: Field

    fun setSwitchListener(settingItem: LSettingItem, listener: View.OnClickListener) {
        init()
        (switchField.get(settingItem) as SwitchCompat).setOnClickListener(listener)
    }

    fun isChecked(settingItem: View): Boolean {
        init()
        return (switchField.get(settingItem) as SwitchCompat).isChecked
    }

    fun turnSettingSwitch(settingItem: LSettingItem, checked: Boolean) {
        init()
        (switchField.get(settingItem) as SwitchCompat).isChecked = checked
        checkOnField.setBoolean(settingItem, checked)
    }

    private fun init() {
        if (!this::switchField.isInitialized) {
            val clz = LSettingItem::class.java
            switchField = clz.getDeclaredField("mRightIcon_switch")
            switchField.isAccessible = true
            checkOnField = clz.getDeclaredField("mChecked")
            checkOnField.isAccessible = true
        }
    }
}