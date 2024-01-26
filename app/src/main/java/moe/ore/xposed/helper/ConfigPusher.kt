package moe.ore.xposed.helper

import de.robv.android.xposed.XposedBridge
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import moe.ore.android.AndroKtx
import moe.ore.txhook.helper.EMPTY_BYTE_ARRAY
import moe.ore.txhook.helper.FileUtil

@ExperimentalSerializationApi
object ConfigPusher {
    const val KEY_FORBID_TCP = "forbid_tcp"
    const val KEY_ECDH_DEFAULT = "ecdh_default"
    const val KEY_PUSH_API = "push_api"
    const val KEY_ECDH_NEW_HOOK = "ecdh_new_hook"
    const val KEY_OPEN_LOG = "output_log"
    const val KEY_WS_ADDRESS = "ws_addr"

    const val KEY_D2_KEY = "d2"
    const val KEY_TGT_KEY = "tgt"

    const val KEY_DATA_PUBLIC = "ecdh_public"
    const val KEY_DATA_SHARE = "ecdh_share"

    const val ALLOW_SOURCE = "find_source"

    fun initForOnce() {
        this[ALLOW_SOURCE] = "yes"
    }

    operator fun get(name: String): String? {
        kotlin.runCatching {
            return getData()?.cfg?.get(name)
        }.onFailure {
            if (it is IllegalStateException) {
                XposedBridge.log("当前设备不具有储存权限")
            }
        }
        return null
    }

    operator fun set(key: String, value: String) {
        val dir: String = AndroKtx.dataDir + "/cfg/config.ini"
        val data = getData() ?: Config()
        data.cfg[key] = value
        FileUtil.saveFile(dir, ProtoBuf.encodeToByteArray(data))
    }

    fun getData(key: String): ByteArray? {
        return getData()?.data?.get(key)
    }

    fun setData(key: String, value: ByteArray) {
        val dir: String = AndroKtx.dataDir + "/cfg/config.ini"
        val data = getData() ?: Config()
        data.data[key] = value
        FileUtil.saveFile(dir, ProtoBuf.encodeToByteArray(data))
    }

    private fun getData(): Config? {
        val dir: String = AndroKtx.dataDir + "/cfg/config.ini"
        if (FileUtil.has(dir)) {
            return ProtoBuf.decodeFromByteArray<Config>(
                FileUtil.readFileBytes(dir)
            )
        }
        return null
    }

    @Serializable
    class Config(
        @ProtoNumber(1) var cfg: HashMap<String, String> = hashMapOf(),
        @ProtoNumber(2) var data: HashMap<String, ByteArray> = hashMapOf()
    )

    fun pushTicket(source: Int, key: String, sig: ByteArray) {
        val map = getData(key)?.let {
            ProtoBuf.decodeFromByteArray<TicketMap>(it)
        } ?: TicketMap()
        map.ticketMap[source] = Ticket(sig)
    }

    fun pushTicket(source: Int, key: String, sig: ByteArray, sig_key: ByteArray) {
        val map = getData(key)?.let {
            ProtoBuf.decodeFromByteArray<TicketMap>(it)
        } ?: TicketMap()
        map.ticketMap[source] = Ticket(sig, sig_key)
    }

    fun popTicketMap(key: String): TicketMap {
        return getData(key)?.let {
            ProtoBuf.decodeFromByteArray<TicketMap>(it)
        } ?: TicketMap()
    }
}

@Serializable
class TicketMap(
    @ProtoNumber(1) var ticketMap: HashMap<Int, Ticket> = hashMapOf(),
)

@Serializable
class Ticket(
    @ProtoNumber(1) var sig: ByteArray = EMPTY_BYTE_ARRAY,
    @ProtoNumber(2) var sig_key: ByteArray = EMPTY_BYTE_ARRAY
)