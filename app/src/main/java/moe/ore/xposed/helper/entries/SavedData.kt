package moe.ore.xposed.helper.entries

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
class SavedDataMap(
    @ProtoNumber(1) @JvmField var dataMap: HashMap<Int, SavedData> = hashMapOf()
) {
    operator fun get(key: Int) = dataMap[key]
    operator fun set(key: Int, data: SavedData) {
        dataMap[key] = data
    }
}

/**
 * key map:
 * appId
 */
@Serializable
class SavedData(
    @ProtoNumber(1) @JvmField var dataMap: HashMap<String, ByteArray> = hashMapOf()
) {
    fun getInt(key: String): Int? = getString(key)?.toInt()

    fun getLong(key: String): Long? = getString(key)?.toLong()

    fun getString(key: String): String? = dataMap[key]?.decodeToString()

    operator fun get(key: String): ByteArray? = dataMap[key]
}
