package moe.ore.txhook.app.fragment

import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import moe.ore.android.toast.Toast
import moe.ore.android.util.AndroidUtil
import moe.ore.txhook.R
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.MQQ
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQHD
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQLITE
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQMUSIC
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQSAFE
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.TIM
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.WEGAME
import moe.ore.txhook.app.ui.info.OnItemClickListener
import moe.ore.txhook.databinding.FragmentDataBinding
import moe.ore.txhook.helper.*
import moe.ore.xposed.helper.*
import moe.ore.xposed.helper.ConfigPusher.KEY_DATA_PUBLIC
import moe.ore.xposed.helper.ConfigPusher.KEY_DATA_SHARE
import moe.ore.xposed.helper.entries.SavedToken
import moe.ore.xposed.helper.entries.Token

@OptIn(ExperimentalSerializationApi::class)
class DataFragment : Fragment() {
    companion object {
        const val PUBLIC = 0.toByte()
        const val SHARE = 1.toByte()
    }

    private var lastSelected: Int = 0
    private val onChanges: AtomicBoolean = atomic(false)
    private lateinit var binding: FragmentDataBinding
    private var source = MQQ
    private lateinit var myPublic: Token
    private lateinit var myShare: Token
    private var isLocking = atomic(false) to atomic(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentDataBinding.inflate(inflater, container, false).also {
        this.binding = it
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uiChange()
        binding.switchButton.setOnSwitchListener { position, _ ->
            if (onChanges.value) {
                binding.switchButton.selectedTab = lastSelected
            } else {
                lastSelected = position
                initSourceValue(position)
                uiChange()
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (!this::binding.isInitialized) return

        kotlin.runCatching {
            initSourceValue(binding.switchButton.selectedTab)
            uiChange()
        }.onFailure {
            it.printStackTrace()
        }
    }

    private inline fun uiChange() {
        onChanges.lazySet(true)
        loadEcdh()
        loadSession()
        onChanges.lazySet(false)
    }

    private fun loadSession() {
        val session = binding.session
        session.clear()
        session.tittle("Session")

        val d2Map = ConfigPusher.popTicketMap(ConfigPusher.KEY_D2_KEY)
        val tgtMap = ConfigPusher.popTicketMap(ConfigPusher.KEY_TGT_KEY)

        val d2 = d2Map.ticketMap[source]
        val tgt = tgtMap.ticketMap[source]

        if (d2 != null && tgt != null) {
            session.item(R.drawable.ic_baseline_key_24, "D2SIG", d2.sig.toHexString())
            session.item(R.drawable.ic_baseline_key_24, "D2KEY", d2.sig_key.toHexString())
            session.item(R.drawable.ic_baseline_key_24, "TGTSIG", tgt.sig.toHexString())
            session.item(R.drawable.ic_baseline_key_24, "TGTKEY", tgt.sig_key.toHexString())
            session.hideNoData()
        } else session.showNoData()
    }

    private fun loadEcdh() {
        val ecdh = binding.ecdh
        ecdh.clear()
        ecdh.tittle("EcdhCrypt")

        val public: SavedToken = ConfigPusher.getData(KEY_DATA_PUBLIC).let {
            if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
        }
        val share: SavedToken = ConfigPusher.getData(KEY_DATA_SHARE).let {
            if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
        }
        this.myPublic = public[source]
        this.myShare = share[source]
        isLocking.first.lazySet(myPublic.isLock)

        if (myShare.token.isNotEmpty()) {
            ecdh.item(
                if (myPublic.isLock) R.drawable.ic_lock else R.drawable.ic_unlock,
                "PublicKey", myPublic.token.toHexString(), EcdhItemClickListener(PUBLIC)
            )

            isLocking.second.lazySet(myShare.isLock)
            ecdh.item(
                if (myShare.isLock) R.drawable.ic_lock else R.drawable.ic_unlock,
                "ShareKey", myShare.token.toHexString(), EcdhItemClickListener(SHARE)
            )
            ecdh.hideNoData()
        } else ecdh.showNoData()
    }

    private fun initSourceValue(position: Int) {
        this.source = when (position) {
            0 -> MQQ
            // qq hd
            1 -> QQHD
            // qqlite
            2 -> QQLITE
            // tim
            3 -> TIM
            4 -> WEGAME
            5 -> QQMUSIC
            6 -> QQSAFE
            else -> position
        }
    }

    inner class EcdhItemClickListener(
        private val keyType: Byte
    ) : OnItemClickListener {
        override fun onClickItem() {
            if (keyType == PUBLIC)
                AndroidUtil.copyText(requireContext(), myPublic.token.toHexString())
            else if (keyType == SHARE)
                AndroidUtil.copyText(requireContext(), myShare.token.toHexString())
        }

        override fun onClickLeftIcon(image: ImageView) {
            if (keyType == PUBLIC) {
                if (isLocking.first.value) {
                    image.setImageResource(R.drawable.ic_unlock)
                    ConfigPusher.setData(KEY_DATA_PUBLIC, ProtoBuf.encodeToByteArray(
                        ConfigPusher.getData(KEY_DATA_PUBLIC).let {
                            if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
                        }.also { sv ->
                            sv[source].also { tk ->
                                tk.isLock = false
                            }
                        }
                    ))
                    Toast.toast(requireContext(), "解锁成功")
                    isLocking.first.lazySet(false)
                } else {
                    image.setImageResource(R.drawable.ic_lock)
                    ConfigPusher.setData(KEY_DATA_PUBLIC, ProtoBuf.encodeToByteArray(
                        ConfigPusher.getData(KEY_DATA_PUBLIC).let {
                            if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
                        }.also { sv ->
                            sv[source].also { tk ->
                                tk.isLock = true
                            }
                        }
                    ))
                    Toast.toast(requireContext(), "上锁成功")
                    isLocking.first.lazySet(true)
                }
            } else if (keyType == SHARE) {
                if (isLocking.second.value) {
                    image.setImageResource(R.drawable.ic_unlock)
                    ConfigPusher.setData(KEY_DATA_SHARE, ProtoBuf.encodeToByteArray(
                        ConfigPusher.getData(KEY_DATA_SHARE).let {
                            if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
                        }.also { sv ->
                            sv[source].also { tk ->
                                tk.isLock = false
                            }
                        }
                    ))
                    Toast.toast(requireContext(), "解锁成功")
                    isLocking.second.lazySet(false)
                } else {
                    image.setImageResource(R.drawable.ic_lock)
                    ConfigPusher.setData(KEY_DATA_SHARE, ProtoBuf.encodeToByteArray(
                        ConfigPusher.getData(KEY_DATA_SHARE).let {
                            if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
                        }.also { sv ->
                            sv[source].also { tk ->
                                tk.isLock = true
                            }
                        }
                    ))
                    Toast.toast(requireContext(), "上锁成功")
                    isLocking.second.lazySet(true)
                }
            }
        }
    }
    /*
    private fun loadEcdh() {
        val pub = EcdhFucker.getPublicKey()
        val sha = EcdhFucker.getShareKey()

        val pubText = pub.key.toHexString()
        val shaText = sha.key.toHexString()

        binding.publicKey.text = pubText.let { if (it.length <= 32) it else it.substring(0, 32) + "..." }
        binding.shareKey.text = shaText.let { if (it.length <= 32) it else it.substring(0, 32) + "..." }

        binding.lockPublic.also {
            it.setImageDrawable(
                if (pub.lock)
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_lock, null)
                else ResourcesCompat.getDrawable(resources, R.drawable.ic_unlock, null)
            )
        }.setOnClickListener {
            EcdhFucker.setLockPublic(pub.key, !pub.lock)
            Toast.toast(msg = if (pub.lock) "unlock" else "lock")
            loadEcdh()
        }

        binding.lockShare.also {
            it.setImageDrawable(
                if (sha.lock)
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_lock, null)
                else ResourcesCompat.getDrawable(resources, R.drawable.ic_unlock, null)
            )
        }.setOnClickListener {
            EcdhFucker.setLockShare(sha.key, !sha.lock)
            Toast.toast(msg = if (sha.lock) "unlock" else "lock")
            loadEcdh()
        }

        binding.publicView.setOnClickListener {
            AndroidUtil.copyText(requireContext(), pubText)
        }
        binding.shareView.setOnClickListener {
            AndroidUtil.copyText(requireContext(), shaText)
        }
    } */
}
