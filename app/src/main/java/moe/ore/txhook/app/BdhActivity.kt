@file:OptIn(ExperimentalSerializationApi::class)

package moe.ore.txhook.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.yuyh.jsonviewer.library.moved.ProtocolViewer
import kotlinx.io.core.discardExact
import kotlinx.io.core.readBytes
import kotlinx.serialization.ExperimentalSerializationApi
import moe.ore.android.Application
import moe.ore.android.EasyActivity
import moe.ore.android.toast.Toast
import moe.ore.android.util.StatusBarUtil
import moe.ore.txhook.R
import moe.ore.txhook.app.fragment.MainFragment
import moe.ore.txhook.app.fragment.PacketHexFragment
import moe.ore.txhook.databinding.ActivityPacketBinding
import moe.ore.txhook.databinding.FragmentPacketInfoBinding
import moe.ore.txhook.databinding.FragmentParserBinding
import moe.ore.txhook.helper.parser.ProtobufParser
import moe.ore.txhook.helper.toByteReadPacket
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

@ExperimentalSerializationApi
class BdhActivity : EasyActivity() {
    private val mFragmentList: ArrayList<Fragment> = ArrayList()
    private lateinit var binding: ActivityPacketBinding
    private lateinit var titles: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StatusBarUtil.setStatusBarColor(
            this,
            ResourcesCompat.getColor(resources, R.color.white, null)
        )
        StatusBarUtil.setAndroidNativeLightStatusBar(this, true)

        binding = ActivityPacketBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bdhData = intent.getParcelableExtra<MainFragment.BdhData>("data")
            ?: error("bdh activity data field is must not null")

        titles = if (bdhData.isSend) arrayOf("详细", "解析", "内容") else arrayOf(
            "详细",
            "RESP",
            "EXTEND"
        )

        mFragmentList.add(MyBdhGetFragment().apply { this.bdhData = bdhData })
        if (bdhData.isSend) {
            mFragmentList.add(MyParserFragment().also { it.bdhData = bdhData })
        }
        mFragmentList.add(PacketHexFragment().also { it.initBuffer(bdhData.data) })
        if (!bdhData.isSend) {
            mFragmentList.add(PacketHexFragment().also { it.initBuffer(bdhData.extendInfo) })
        }

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

class MyParserFragment : Fragment() {
    companion object {
        private val empty = JSONObject(
            hashMapOf(
                "fuck" to true
            ) as Map<*, *>
        )
    }

    lateinit var bdhData: MainFragment.BdhData
    lateinit var head: ByteArray
    lateinit var tail: ByteArray
    private lateinit var binding: FragmentParserBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentParserBinding.inflate(inflater, container, false).also {
            this.binding = it
            binding.jce.text = "解析头部"
            binding.pb.text = "解析尾部"
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.bdhData = if (this::bdhData.isInitialized)
            bdhData else savedInstanceState?.getParcelable("data")!!
        savedInstanceState?.getByteArray("head")?.let { this.head = it }
        savedInstanceState?.getByteArray("tail")?.let { this.tail = it }

        val parser = binding.parser
        parser.setTextSize(16f)
        parser.setScaleEnable(true)
        parser.setOnBindListener(object : ProtocolViewer.OnBindListener {
            override fun onBindString(json: String?) {
                if (json != null) show() else hide()
            }

            override fun onBindObject(json: JSONObject?) {
                if (json != null) {
                    if (json.optBoolean("fuck", false)) hide() else show()
                } else hide()
            }

            override fun onBindArray(json: JSONArray?) {
                if (json != null) show() else hide()
            }

            private fun show() {
                binding.parseData.visibility = View.VISIBLE

            }

            private fun hide() {
                binding.parseData.visibility = View.GONE

            }
        })
        parser.bindJson(empty)

        binding.jce.setOnClickListener {
            thread(isDaemon = true) {
                if (!this::head.isInitialized) {
                    val reader = bdhData.data.toByteReadPacket()
                    reader.discardExact(1)
                    val size = reader.readInt()
                    reader.discardExact(4)
                    this.head = reader.readBytes(size)
                    reader.close()
                }
                kotlin.runCatching { ProtobufParser(head).start() }.also {
                    Application.uiHandler.post {
                        it.onFailure {
                            it.printStackTrace()
                            Toast.toast(context, "头部分析失败")
                        }.onSuccess {
                            Toast.toast(context, "头部分析成功")
                            parser.bindJson(it)
                        }
                    }
                }
            }
        }

        binding.pb.setOnClickListener {
            thread(isDaemon = true) {
                if (!this::tail.isInitialized) {
                    val reader = bdhData.data.toByteReadPacket()
                    reader.discardExact(1)
                    val s1 = reader.readInt()
                    val size = reader.readInt()
                    reader.discardExact(s1)
                    this.tail = reader.readBytes(size)
                    reader.close()
                }
                kotlin.runCatching {
                    ProtobufParser(tail).start()
                }.also {
                    Application.uiHandler.post {
                        it.onFailure {
                            it.printStackTrace()
                            Toast.toast(context, "尾部分析失败")
                        }.onSuccess {
                            Toast.toast(context, "尾部分析成功")
                            parser.bindJson(it)
                        }
                    }
                }
            }
        }

        binding.clear.setOnClickListener {
            parser.bindJson(empty)
            Toast.toast(context, "清空成功")
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (this::bdhData.isInitialized)
            outState.putParcelable("data", bdhData)
        if (this::head.isInitialized)
            outState.putByteArray("head", head)
        if (this::tail.isInitialized)
            outState.putByteArray("tail", tail)
    }

}

@ExperimentalSerializationApi
class MyBdhGetFragment : Fragment() {
    lateinit var bdhData: MainFragment.BdhData
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

        this.bdhData =
            if (this::bdhData.isInitialized) bdhData else savedInstanceState?.getParcelable("data")!!

        val baseInfo = binding.baseInfo
        baseInfo.tittle("基础信息")
        baseInfo.item("Cmd", bdhData.cmd)
        baseInfo.item("CmdId", bdhData.cmdId.toString())
        baseInfo.item("Seq", bdhData.seq.toString())
        baseInfo.item("DataSize", bdhData.data.size.toString())

        val plusInfo = binding.plusInfo
        plusInfo.tittle("附加信息")
        plusInfo.item(
            "会话来源", when (bdhData.source) {
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
        outState.putParcelable("data", bdhData)
    }
}
