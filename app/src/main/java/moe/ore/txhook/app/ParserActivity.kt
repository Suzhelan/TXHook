package moe.ore.txhook.app

import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import moe.ore.android.EasyActivity
import moe.ore.android.toast.Toast
import moe.ore.android.util.StatusBarUtil
import moe.ore.txhook.R
import moe.ore.txhook.app.fragment.MainFragment
import moe.ore.txhook.databinding.ActivityParserBinding
import moe.ore.txhook.helper.parser.ProtobufParser
import moe.ore.txhook.helper.parser.TarsParser
import moe.ore.txhook.helper.toByteReadPacket

class ParserActivity : EasyActivity() {
    private lateinit var binding: ActivityParserBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        StatusBarUtil.setStatusBarColor(
            this,
            ResourcesCompat.getColor(resources, R.color.white, null)
        )
        StatusBarUtil.setAndroidNativeLightStatusBar(this, true)

        binding = ActivityParserBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val packet = intent.getParcelableExtra<MainFragment.Packet>("data")
            ?: error("packet activity packet field is must not null")
        val isJce = intent.getBooleanExtra("jce", false)

        kotlin.runCatching {
            val buffer = packet.buffer
            val index = buffer.toByteReadPacket().use {
                if (it.readInt() == buffer.size) 4 else 0
            }
            if (isJce) TarsParser(buffer, index).start() else ProtobufParser(buffer, index).start()
        }.onSuccess {
            binding.parser.bindJson(it)
        }.onFailure {
            Toast.toast(msg = "解析失败")
            finish()
        }

        binding.back.setOnClickListener { finish() }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}