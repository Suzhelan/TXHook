package moe.ore.xposed.helper.entries

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import moe.ore.txhook.helper.EMPTY_BYTE_ARRAY

@Serializable
class SavedToken(
    @ProtoNumber(1) @JvmField var tokenMap: HashMap<Int // source
            , Token> = hashMapOf()
) {
    operator fun get(int: Int) = tokenMap.getOrPut(int) { Token() }
}

@Serializable
data class Token(
    @ProtoNumber(1) @JvmField var token: ByteArray = EMPTY_BYTE_ARRAY,
    @ProtoNumber(2) @JvmField var isLock: Boolean = false,
)
