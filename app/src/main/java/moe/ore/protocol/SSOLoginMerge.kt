package moe.ore.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import moe.ore.txhook.helper.EMPTY_BYTE_ARRAY

class SSOLoginMerge {
    @Serializable
    data class BusiBuffData(
        @ProtoNumber(number = 1) @JvmField var buffList: ArrayList<BusiBuffItem>? = null,
        @ProtoNumber(number = 2) @JvmField var MaxRespSizeHint: Int? = null,
    )

    @Serializable
    data class BusiBuffItem(
        @ProtoNumber(number = 1) @JvmField var seq: Int = 0,
        @ProtoNumber(number = 2) @JvmField var cmd: String = "",
        @ProtoNumber(number = 3) @JvmField var size: Int = 0,
        @ProtoNumber(number = 4) @JvmField var data: ByteArray = EMPTY_BYTE_ARRAY,
        @ProtoNumber(number = 5) @JvmField var needResp: Boolean = false
    )
}