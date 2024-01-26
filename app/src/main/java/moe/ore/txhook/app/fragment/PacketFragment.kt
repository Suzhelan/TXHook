package moe.ore.txhook.app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.yuyh.jsonviewer.library.moved.ProtocolViewer
import kotlinx.serialization.ExperimentalSerializationApi
import moe.ore.android.Application.Companion.uiHandler
import moe.ore.android.toast.Toast
import moe.ore.android.util.AndroidUtil
import moe.ore.txhook.R
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.MQQ
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQHD
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQLITE
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQMUSIC
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQSAFE
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQWATCH
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.TIM
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.WEGAME
import moe.ore.txhook.app.ui.info.OnItemClickListener
import moe.ore.txhook.databinding.FragmentHexBinding
import moe.ore.txhook.databinding.FragmentPacketInfoBinding
import moe.ore.txhook.databinding.FragmentParserBinding
import moe.ore.txhook.helper.hashCode
import moe.ore.txhook.helper.parser.ProtobufParser
import moe.ore.txhook.helper.parser.TarsParser
import moe.ore.txhook.helper.toByteReadPacket
import moe.ore.txhook.helper.toHexString
import moe.ore.xposed.helper.SourceFinder
import org.json.JSONArray
import org.json.JSONObject
import kotlin.concurrent.thread

class PacketHexFragment : Fragment() {
    private lateinit var binding: FragmentHexBinding

    private var hex: String? = null
    private var size: Int = 0

    fun initBuffer(buffer: ByteArray) {
        size = buffer.size
        hex = buffer.toHexString(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentHexBinding.inflate(inflater, container, false).also {
            this.binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (hex == null) {
            size = savedInstanceState?.getInt("size") ?: 0
            hex = savedInstanceState?.getString("hex") ?: ""
        }

        binding.info.text = "${size}字节 | 长按可复制"
        binding.hex.text = hex
        binding.hex.typeface = ResourcesCompat.getFont(requireContext(), R.font.mono)

        binding.copy.setOnClickListener {
            AndroidUtil.copyText(requireContext(), binding.hex.text)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("size", size)
        outState.putString("hex", hex)
    }
}

class PacketInfoFragment : Fragment() {
    lateinit var packet: MainFragment.Packet
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

    @OptIn(ExperimentalSerializationApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.packet =
            if (this::packet.isInitialized) packet else savedInstanceState?.getParcelable("data")!!

        val baseInfo = binding.baseInfo
        baseInfo.tittle("基础信息")
        baseInfo.item("指令名称", packet.cmd, clickListener = object : OnItemClickListener {
            override fun onClickItem() {
                AndroidUtil.copyText(context, packet.cmd)
            }
        })
        baseInfo.item("用户标识", packet.uin.toString())
        baseInfo.item("操作时间", packet.time.toString())
        baseInfo.item("自增序列", packet.seq.toString())
        baseInfo.item("包体大小", packet.buffer.size.toString())

        /*
        binding.cmd.text = packet.cmd
        binding.uin.text = packet.uin.toString()
        binding.time.text = packet.time.toString()
        binding.seq.text = packet.seq.toString()
        binding.size.text = packet.buffer.size.toString()
        binding.cookie.text = packet.msgCookie.toHexString()
        binding.type.text = packet.type */

        val plusInfo = binding.plusInfo
        plusInfo.tittle("附加信息")
        plusInfo.item("会话标识", packet.msgCookie.toHexString())
        plusInfo.item("会话类型", packet.type)
        kotlin.runCatching {
            var hash = packet.hash
            if (packet.hash == 0) {
                hash = packet.buffer.contentHashCode()
            }
            var find = SourceFinder.find(hash)
            if (find == null) {
                hash = packet.buffer.hashCode(4)
                find = SourceFinder.find(hash)
            }
            if (find != null) {
                plusInfo.item("组包来源", find)
            }
            packet.hash = hash
        }
        plusInfo.item(
            "会话来源", when (packet.source) {
                MQQ -> "MobileQQ"
                QQHD -> "QQHD"
                QQLITE -> "QQLite"
                QQWATCH -> "QQWatch"
                TIM -> "Tim"
                WEGAME -> "WeGame"
                QQSAFE -> "QQSafe"
                QQMUSIC -> "QQMusic"
                else -> "unknown"
            }
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("data", packet)
    }
}

class ParserFragment : Fragment() {
    companion object {
        private val empty = JSONObject(
            hashMapOf(
                "fuck" to true
            ) as Map<*, *>
        )
    }

    lateinit var packet: MainFragment.Packet
    private lateinit var binding: FragmentParserBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentParserBinding.inflate(inflater, container, false).also {
            this.binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.packet =
            if (this::packet.isInitialized) packet else savedInstanceState?.getParcelable("data")!!

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
                binding.parseData.visibility = VISIBLE

            }

            private fun hide() {
                binding.parseData.visibility = GONE

            }
        })
        parser.bindJson(empty)

        binding.jce.setOnClickListener {
            thread(isDaemon = true) {
                kotlin.runCatching {
                    val buffer = packet.buffer
                    TarsParser(buffer, buffer.toByteReadPacket().use {
                        if (it.readInt() == buffer.size) 4 else 0
                    }).start()
                }.also {
                    uiHandler.post {
                        it.onFailure {
                            Toast.toast(context, "Tars分析失败")
                        }.onSuccess {
                            Toast.toast(context, "Tars分析成功")
                            parser.bindJson(it)
                        }
                    }
                }
            }
        }

        binding.pb.setOnClickListener {
            thread(isDaemon = true) {
                kotlin.runCatching {
                    val buffer = packet.buffer
                    ProtobufParser(buffer, buffer.toByteReadPacket().use {
                        if (it.readInt() == buffer.size) 4 else 0
                    }).start()
                }.also {
                    uiHandler.post {
                        it.onFailure {
                            it.printStackTrace()
                            Toast.toast(context, "Pb分析失败: ")
                        }.onSuccess {
                            Toast.toast(context, "Pb分析成功")
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
        outState.putParcelable("data", packet)
    }

}