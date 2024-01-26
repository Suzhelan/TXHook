package moe.ore.script

object Consist {
    var isCatch: Boolean = false

    val titles = arrayOf("主页", "数据", "工具", "设置")

    const val GET_TEST_DATA = "get_test_data"
    const val GET_TXHOOK_STATE = "get_txhook_state"
    const val GET_TXHOOK_WS_STATE = "get_txhook_ws_state"
    const val MMKV_HANDLE_MERGE = "handle_merge"

    const val MMKV_MERGE_PROTOBUF = "merge_protobuf"

    const val MMKV_QLOG = "qlog_out"
}