package moe.ore.txhook.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.serialization.ExperimentalSerializationApi
import moe.ore.android.EasyActivity
import moe.ore.android.util.StatusBarUtil
import moe.ore.txhook.R
import moe.ore.txhook.app.fragment.MainFragment
import moe.ore.txhook.app.fragment.PacketHexFragment
import moe.ore.txhook.databinding.ActivityPacketBinding
import moe.ore.txhook.databinding.FragmentPacketInfoBinding

@ExperimentalSerializationApi
class TlvActivity : EasyActivity() {
    private val mFragmentList: ArrayList<Fragment> = ArrayList()
    private lateinit var binding: ActivityPacketBinding
    private val titles = arrayOf("详细", "内容")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StatusBarUtil.setStatusBarColor(
            this,
            ResourcesCompat.getColor(resources, R.color.white, null)
        )
        StatusBarUtil.setAndroidNativeLightStatusBar(this, true)

        binding = ActivityPacketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val packet = intent.getParcelableExtra<MainFragment.Action>("data")
            ?: error("packet activity packet field is must not null 2")

        mFragmentList.add(TlvGetFragment().apply { this.action = packet })
        mFragmentList.add(PacketHexFragment().also { it.initBuffer(packet.buffer) })

        val viewPager = binding.viewPager
        val adapter: FragmentStateAdapter =
            object : FragmentStateAdapter(supportFragmentManager, lifecycle) {
                override fun getItemCount(): Int = mFragmentList.size

                override fun createFragment(position: Int): Fragment = mFragmentList[position]
            }
        viewPager.adapter = adapter
        val tabLayout = binding.tabs

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = titles[position]
        }.attach()

        binding.back.setOnClickListener { finish() }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

@ExperimentalSerializationApi
class TlvGetFragment : Fragment() {
    lateinit var action: MainFragment.Action
    private lateinit var binding: FragmentPacketInfoBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentPacketInfoBinding.inflate(inflater, container, false).also {
            this.binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.action =
            if (this::action.isInitialized) action else savedInstanceState?.getParcelable("data")!!

        val baseInfo = binding.baseInfo
        baseInfo.tittle("基础信息")
        baseInfo.item("TYPE", "0x" + Integer.toHexString(action.what))

        val plusInfo = binding.plusInfo
        plusInfo.tittle("附加信息")
        plusInfo.item(
            "会话来源", when (action.source) {
                MainFragment.Packet.MQQ -> "MobileQQ"
                MainFragment.Packet.QQHD -> "QQHD"
                MainFragment.Packet.QQLITE -> "QQLite"
                MainFragment.Packet.QQWATCH -> "QQWatch"
                MainFragment.Packet.TIM -> "Tim"
                MainFragment.Packet.WEGAME -> "WeGame"
                MainFragment.Packet.QQSAFE -> "QQSafe"
                MainFragment.Packet.QQMUSIC -> "QQMusic"
                else -> "unknown"
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("data", action)
    }
}
