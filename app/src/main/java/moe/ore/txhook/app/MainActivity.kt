@file:OptIn(ExperimentalSerializationApi::class)

package moe.ore.txhook.app

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.KeyEvent
import androidx.core.app.WindowsCompat
import androidx.core.app.WindowsCompat.transWindowsHide
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.serialization.ExperimentalSerializationApi
import moe.ore.android.AndroKtx
import moe.ore.android.EasyActivity
import moe.ore.android.tab.TabFlashyAnimator
import moe.ore.android.toast.Toast.toast
import moe.ore.android.util.StatusBarUtil
import moe.ore.script.Consist
import moe.ore.txhook.EntryActivity
import moe.ore.txhook.R
import moe.ore.txhook.app.fragment.*
import moe.ore.txhook.databinding.ActivityMainBinding
import moe.ore.txhook.helper.HexUtil
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import kotlin.concurrent.thread
import kotlin.system.exitProcess

class MainActivity : EasyActivity() {
    companion object {
        val DEFAULT_PUBLIC_KEY =
            HexUtil.Hex2Bin("30819F300D06092A864886F70D010101050003818D0030818902818100AA00F323F2DC4BBD0680CBC9FADF7AF5A8ED2E2962D44C38157B37260BD65EBFA6237258F2CC55E86B15007FA9D13E2701C3A88AD292FDD175858489D9587E7A3BA555956E9FC5F602A7A4F489C447782AC2647212B494A9EF4EC44DF82957FF405F7BB6C920D98C9D98645AFEF34FC003C707A1D2FF426CF7EAE0BDBD51D5FF0203010001")
        val DEFAULT_PRIVATE_KEY = HexUtil.Hex2Bin(
            "30820276020100300D06092A864886F70D0101010500048202603082025C02010002818100AA00F323F2DC4BBD0680CBC9FADF7AF5A8ED2E2962D44C38157B37260BD65EBFA6237258F2CC55E86B15007FA9D13E2701C3A88AD292FDD175858489D9587E7A3BA555956E9FC5F602A7A4F489C447782AC2647212B494A9EF4EC44DF82957FF405F7BB6C920D98C9D98645AFEF34FC003C707A1D2FF426CF7EAE0BDBD51D5FF020301000102818001CD86C68FD1C43FD9ECCDBC739BA11B2FD26C15E6456815842CCD55EAF43807024507F66784C13878C23D421D53E9BBD229F80498DD1431FF740E06C4364B090E5F288E700AF03A29B35BB43880786F2F2085E2FE28A700461075431D3ABF26BF241A4ED63CDD3A0B4B141C78DC150E392F16BA2A1EE3A823CB0B951ECAF409024100E05661CA5A2E474B160827851522D588CE0A9AC106268AEEEE0D5AB4A44432B1B57BEFCE44C6A0946B91B35088D1C1EAE1535D1BECB61FAB7E29B600CF9D6B7B024100C1FF6A8C9BDB1C483BFE5FC1270606DFAF55CD8DABCF8B6A502E23670D395B4498346AC78883ACA9EA2753473FC7B58053B1F2FE678E192B0664F13714B8E64D024059259A88A9DB78133B7714154B77E33910FF9FCD929F2058A01A886FFE52E77E3CEB3A39529547DC92FE7C2E45A06D19E45E974270875300780B253B1F45A41F0240480E2C6F297C8AD6B1A1DBC30C518AC00E89DA1D62D165C10922F9F74ECC1D002F6058C0E00DB8562C288B200DAA89D9AE3C8C3ABE0FE37D3D94C49B66D0FE890241009724365926B7E1063C41C552D5C9B45BCCA2197DBE27FBDC7D3D214387312243A31BCDC02125B2A7D83CD44FD995FDC25A7125B6C7FD732408C8CAE80C7CB220"
        )
    }

    private val authCheck = fun(context: Context) {

        fun decode(data: ByteArray): ByteArray {
            val keySpec = PKCS8EncodedKeySpec(DEFAULT_PRIVATE_KEY)
            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKey = keyFactory.generatePrivate(keySpec)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            return cipher.doFinal(data)
        }

        fun encode(data: ByteArray): ByteArray {
            val keyFactory = KeyFactory.getInstance("RSA")
            val keySpec = X509EncodedKeySpec(DEFAULT_PUBLIC_KEY)
            val publicKey = keyFactory.generatePublic(keySpec)
            val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
            cipher.init(Cipher.ENCRYPT_MODE, publicKey)
            return cipher.doFinal(data)
        }

        val auth = context.getSharedPreferences("auth", 0).getString("auth", null)

        if (auth.isNullOrEmpty()) exitProcess(1)
        else thread {
            // val out = String(decode(HttpUtil.do("https://xinrao.co/txhook/?a=" + encode(auth.toByteArray()).toHexString() + "&c=1&d=13001&act=GetCloudAlgorithms").body!!.string().hex2ByteArray()))
            // if (!out.startsWith("null")) error("ill")
        }
    }
    lateinit var binding: ActivityMainBinding

    private val mFragmentList: ArrayList<Fragment> = ArrayList()
    internal lateinit var tabFlashyAnimator: TabFlashyAnimator
    private val titles = arrayOf("主页", "数据", "工具", "设置")

    private var isExit = 0
    private val exitHandler: Handler by lazy {
        object : Handler(mainLooper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                isExit--
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!AndroKtx.isInit) {
            val intent = Intent(this, EntryActivity::class.java)
            startActivity(intent)
            finish()
        }

        super.onCreate(savedInstanceState)

        StatusBarUtil.transparentStatusBar(this)
        StatusBarUtil.setAndroidNativeLightStatusBar(this, true)

        transWindowsHide { WindowsCompat.WINDOWS_FLAG }
        setContentView(binding.root)

        val selectedColor = Color.rgb(105, 105, 105)

        val toolbar = binding.toolbar
        toolbar.setTitleTextColor(selectedColor)
        toolbar.setSubtitleTextColor(selectedColor)
        toolbar.subtitle = "代开源 : github/Suzhelan/TXHook"
        toolbar.setOnClickListener {
            val intent = Intent()
            intent.setAction("android.intent.action.VIEW")
            val content_url = Uri.parse("https://github.com/Suzhelan/TXHook")
            intent.setData(content_url)
            startActivity(intent)
        }
        setSupportActionBar(toolbar)

        mFragmentList.add(MainFragment())
        mFragmentList.add(DataFragment())
        mFragmentList.add(ToolsFragment(titles[2]))
        mFragmentList.add(SettingFragment())

        val viewPager = binding.viewPager
        val adapter: FragmentStateAdapter =
            object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
                override fun getItemCount(): Int = mFragmentList.size

                override fun createFragment(position: Int): Fragment = mFragmentList[position]
            }
        viewPager.adapter = adapter
        val tabLayout = binding.tabLayout

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = Consist.titles[position]
        }.attach()

        tabFlashyAnimator = TabFlashyAnimator(tabLayout, selectedColor)
        tabFlashyAnimator.addTabItem(titles[0], R.drawable.ic_events)
        tabFlashyAnimator.addTabItem(titles[1], R.drawable.ic_datas)
        tabFlashyAnimator.addTabItem(titles[2], R.drawable.ic_tools)
        tabFlashyAnimator.addTabItem(titles[3], R.drawable.ic_setting)
        tabFlashyAnimator.highLightTab(0)

        val fab = binding.fab
        fab.imageTintList =
            ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.white, null))
        fab.setOnClickListener {
            val button = it as FloatingActionButton
            if (Consist.isCatch) {
                Consist.isCatch = false
                button.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.icon_nocatch,
                        null
                    )
                )
                button.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.accent,
                        null
                    )
                )
            } else {
                Consist.isCatch = true
                button.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.icon_catch,
                        null
                    )
                )
                button.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.blue500,
                        null
                    )
                )
            }
        }

        viewPager.registerOnPageChangeCallback(tabFlashyAnimator)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 0) {
                    fab.show()
                } else {
                    fab.hide()
                }
                if (position == 1)
                    mFragmentList[1].onHiddenChanged(true)
            }
        })
    }

    override fun onStart() {
        super.onStart()

        // authCheck.invoke(this)
        transWindowsHide { WindowsCompat.WINDOWS_FLAG + 1 }
    }

    override fun onStop() {
        super.onStop()
        tabFlashyAnimator.onStop()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            isExit++
            exit()
            return false
        }
        return super.onKeyDown(keyCode, event)
    }


    private fun exit() {
        if (isExit < 2) {
            toast(msg = "再按一次退出应用")
            exitHandler.sendEmptyMessageDelayed(0, 2000)
        } else {
            finish()
            super.onBackPressed()
        }
    }
}