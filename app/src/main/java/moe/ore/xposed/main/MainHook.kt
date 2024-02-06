@file:Suppress("LocalVariableName", "SpellCheckingInspection", "UNCHECKED_CAST")
@file:OptIn(ExperimentalSerializationApi::class)

package moe.ore.xposed.main

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.ActivityCompat
import com.google.gson.JsonObject
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.XposedHelpers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import moe.ore.android.AndroKtx
import moe.ore.android.toast.Toast
import moe.ore.txhook.app.CatchProvider
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQMUSIC
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.QQSAFE
import moe.ore.txhook.app.fragment.MainFragment.Packet.CREATOR.WEGAME
import moe.ore.txhook.helper.*
import moe.ore.xposed.Initiator.load
import moe.ore.xposed.helper.ConfigPusher
import moe.ore.xposed.helper.ConfigPusher.ALLOW_SOURCE
import moe.ore.xposed.helper.ConfigPusher.KEY_D2_KEY
import moe.ore.xposed.helper.ConfigPusher.KEY_DATA_PUBLIC
import moe.ore.xposed.helper.ConfigPusher.KEY_DATA_SHARE
import moe.ore.xposed.helper.ConfigPusher.KEY_ECDH_DEFAULT
import moe.ore.xposed.helper.ConfigPusher.KEY_ECDH_NEW_HOOK
import moe.ore.xposed.helper.ConfigPusher.KEY_OPEN_LOG
import moe.ore.xposed.helper.ConfigPusher.KEY_TGT_KEY
import moe.ore.xposed.helper.ConfigPusher.KEY_WS_ADDRESS
import moe.ore.xposed.helper.DataKind
import moe.ore.xposed.helper.DataPutter
import moe.ore.xposed.helper.IgnoreCmd.isIgnore
import moe.ore.xposed.helper.SourceFinder
import moe.ore.xposed.helper.entries.SavedToken
import moe.ore.xposed.util.*
import java.io.File
import java.lang.ref.WeakReference
import java.lang.reflect.Field
import kotlin.collections.filter
import kotlin.collections.first
import kotlin.collections.set

object MainHook {
    private val defaultUri = Uri.parse("content://${CatchProvider.MY_URI}")
    private var isInit: Boolean = false
    private var source = 0

    /**
     * 缓存一些用完释放的东西
     */
    private val GlobalCache: HashMap<Int, Any> = hashMapOf()
    private val CodecWarpper = load("com.tencent.qphone.base.util.CodecWarpper")!!
    private val cryptor = load("oicq.wlogin_sdk.tools.cryptor")!!
    // private val fromService = load("com.tencent.qphone.base.remote.FromServiceMsg")!!
    private val EcdhCrypt = load("oicq.wlogin_sdk.tools.EcdhCrypt")
    private val Ticket = load("oicq.wlogin_sdk.request.Ticket")
    private val util = load("oicq.wlogin_sdk.tools.util")
    private val oicq_request = load("oicq.wlogin_sdk.request.oicq_request")!!
    private val WtloginHelper = load("oicq.wlogin_sdk.request.WtloginHelper")!!
    private val tlv_t = load("oicq.wlogin_sdk.tlv_type.tlv_t")
    private val MD5 = load("oicq.wlogin_sdk.tools.MD5")
    private val TcpProtocolDataCodec =
        load("com.tencent.mobileqq.highway.codec.TcpProtocolDataCodec")
    // private val httpCodecClz = load("com.tencent.mobileqq.highway.codec.HttpProtocolDataCodec")

    private var GlobalData: Class<*>? = null
    private val globalDataFields: HashMap<Int, String> = hashMapOf()

    // private const val fuck = false

    operator fun invoke(source: Int, ctx: Context) {
        AndroKtx.dataDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath + File.separator + "txhook"

        // val classLoader = HookClassloader(ctx.classLoader)
        HookUtil.contentResolver = ctx.contentResolver
        HookUtil.contextWeakReference = WeakReference(ctx)
        log("TXHook数据目录：" + AndroKtx.dataDir)
        this.source = source
        // this.ctx = ctx
        // myLessHook(ctx)

        if (source == WEGAME || source == QQMUSIC || source == QQSAFE) {
            hookTea()
            hookTicket()
            hookQQSafe(ctx)
            hookMD5()
            hookTlv()
        } else {
            if (!isInit)
                checkPermissions(ctx)
            CodecWarpper.hookMethod("init")?.before {
                // context isDebug useSubCodec
                // Toast.toast(ctx, "[${ProcessUtil.getCurrentProcessName(ctx)}] TXHOOK初始化($source)")
                if (it.args.size >= 2) {
                    it.args[1] = true // 强制打开调试模式
                    // if (it.args.size >= 3) it.args[2] = true // test version
                    if (!isInit) {
                        val thisClass = it.thisObject.javaClass
                        // hookReceivePacket(thisClass, bytesClz, fromService)
                        hookReceive(thisClass)
                    }
                }
                hookLog()
            }?.after {
                // 【废弃】构建ws长连接作为数据交互
                // ProtocolDatas.setAppId(codecClazz.callMethod("getAppid") as Int)
                // ProtocolDatas.setMaxPackageSize(codecClazz.callMethod("getMaxPackageSize") as Int)
                if (!isInit) {
                    val url = ConfigPusher[KEY_WS_ADDRESS]
                    if (!url.isNullOrBlank()) {
                        Toast.toast(ctx, "尝试连接WebSocket")
                        HookUtil.tryToConnectWS(url, source)
                    }
                    isInit = true
                }
            }
            hookQQSafe(ctx)
            hookMD5()
            hookTlv()
            hookTea()
            hookSendPacket()
            hookBDH()
            hookParams()
            hookSource()
        }
    }

    private fun hookSource() {
        val cfg = ConfigPusher[ALLOW_SOURCE]
        if (cfg.isNullOrBlank() || cfg != "yes") return
        val MessageMicro = load("com.tencent.mobileqq.pb.MessageMicro")
        MessageMicro.hookMethod("toByteArray")?.after {
            if (it.args.size == 3) {
                val buf = it.args[0] as ByteArray
                /*DataPutter.put(DataKind.MATCH_PACKAGE, newBuilder().apply {
                    writeInt(buf.contentHashCode())
                    val pos = it.thisObject.javaClass.toGenericString()
                    writeShort(pos.length)
                    writeString(pos)
                }.toByteArray())*/
                SourceFinder.pushPb(buf.contentHashCode(), it.thisObject.javaClass.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) it.toGenericString() else it.toString()
                })
            } // if (it.args.size == 3)
        }
        val JceStruct = load("com.qq.taf.jce.JceStruct")
        JceStruct.hookMethod("toByteArray")?.after {
            val buf = it.result as ByteArray
            SourceFinder.pushPb(buf.contentHashCode(), it.thisObject.javaClass.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) it.toGenericString() else it.toString()
            })
        }
    }

    private fun toastLog(ctx: Context, string: String) {
        Toast.toast(ctx, string)
        log(string)
    }

    private var has_permision = false

    private inline fun hasStorePermission(ctx: Context): Boolean {
        if (has_permision) return true
        has_permision = ActivityCompat.checkSelfPermission(
            ctx,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    ctx,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
        return has_permision
    }

    private inline fun requestStorePermission(ctx: Context) {
        kotlin.runCatching {
            ActivityCompat.requestPermissions(
                ctx as Activity, arrayOf(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                ), 0
            )
        }.onFailure {
            log(it)
        }
    }

    private fun checkPermissions(ctx: Context) {
        if (!hasStorePermission(ctx)) {
            Toast.toast(ctx, "TXHook需要储存权限才可以运行，请授权对应权限。")
            if (ctx is Activity) {
                requestStorePermission(ctx)
            }
        }
    }

    private fun hookQQSafe(ctx: Context) {
        // 干掉qqanti
        GlobalData = FuzzySearchClass.findClassByField("oicq.wlogin_sdk.request") {
            it.type == ByteArray::class.java && (it.get(null) as? ByteArray)?.contentHashCode() == 881419086
        }

        kotlin.runCatching {
            ChooseUtils.choose(ClassLoader::class.java).forEach {
                kotlin.runCatching {
                    val oicq_request: Class<*> =
                        it.loadClass("oicq.wlogin_sdk.request.oicq_request")
                    val EcdhCrypt: Class<*> = it.loadClass("oicq.wlogin_sdk.tools.EcdhCrypt")
                    val WtloginHelper: Class<*> =
                        it.loadClass("oicq.wlogin_sdk.request.WtloginHelper")
                    hookEcdh(oicq_request, EcdhCrypt, WtloginHelper, ctx)
                }
            }
        }.onFailure {
            EcdhCrypt?.let { hookEcdh(oicq_request, EcdhCrypt, WtloginHelper, ctx) }
        }

        hookTicket()
    }

    val isSomeKey = fun(id: Int, target: Int): Boolean {
        return (id and target) == target
    }

    private inline fun hookTicket() {
        runCatching {
            XposedBridge.hookAllConstructors(Ticket, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val id = param.args[0] as Int
                    if (isSomeKey(id, 262144)) {
                        val sig = param.args[1] as ByteArray
                        val sig_key = param.args[2] as ByteArray
                        ConfigPusher.pushTicket(source, KEY_D2_KEY, sig, sig_key)
                    } else if (isSomeKey(id, 64)) {
                        val sig = param.args[1] as ByteArray
                        val sig_key = param.args[2] as ByteArray
                        ConfigPusher.pushTicket(source, KEY_TGT_KEY, sig, sig_key)
                    }
                }
            })
        }.onFailure {
            log(it)
        }
    }

    private inline fun hookBDH() {
        if (ConfigPusher[ConfigPusher.KEY_FORBID_TCP] == "yes") {
            val pointClz = load("com.tencent.mobileqq.highway.utils.EndPoint")
            val connMng = load("com.tencent.mobileqq.highway.conn.ConnManager")
            connMng.hookMethod("getNextSrvAddr")?.after {
                XposedHelpers.setIntField(it.result, "protoType", 2)
                // toastLog(ctx, it.result.javaClass.toString())
            }
            val tcpConn = load("com.tencent.mobileqq.highway.conn.TcpConnection")
            XposedBridge.hookAllConstructors(tcpConn, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.args.filter { it.javaClass == pointClz }.forEach {
                        XposedHelpers.setIntField(it, "protoType", 2)
                    }
                }
            })
        }
        // tcp 上传hook
        TcpProtocolDataCodec.hookMethod("encodeC2SData")?.after {
            //val endPoint = it.args[0]
            val hwRequest = it.args[1]
            //val data = it.args[2]

            //val isOpenUpEnable = hwRequest.get<Boolean>("isOpenUpEnable")
            //val appId = hwRequest.get<Int>("appid")
            val seq = callMethod(hwRequest, "getHwSeq") as Int
            //val dataFlag = hwRequest.get<Int>("dataFlag")
            //val retryTime = hwRequest.get<Int>("retryCount")
            //val commandId = hwRequest.get<Int>("mBuCmdId")
            //val account = hwRequest.get<String>("account")
            //val cmd = hwRequest.get<String>("hwCmd")

            val commandId = XposedHelpers.getIntField(hwRequest, "mBuCmdId")
            // hwRequest.get<Int>("mBuCmdId")
            //val account = hwRequest.get<String>("account")
            val cmd = XposedHelpers.getObjectField(hwRequest, "hwCmd") as? String
            val data = HookUtil.castToBytes(it.result)

            val values = ContentValues()
            // values.put("uin", account)
            values.put("seq", seq)

            values.put("cmd", cmd)
            values.put("cmdId", commandId)
            values.put("data", data)

            values.put("mode", "bdh.send")

            HookUtil.sendTo(defaultUri, values, source)
        }
        TcpProtocolDataCodec.hookMethod("decodePackage")?.after {
            val dataList = it.args[1] as List<*>

            dataList.forEach {
                if (it == null) return@forEach
                val seq = XposedHelpers.getIntField(it, "hwSeq")
                val respData = XposedHelpers.getObjectField(it, "mRespData") as? ByteArray
                val exData = XposedHelpers.getObjectField(it, "mBuExtendinfo") as? ByteArray
                val cmd = XposedHelpers.getObjectField(it, "cmd") as? String
                val cmdId = XposedHelpers.getIntField(it, "mBuCmdId")
                val retCode = XposedHelpers.getIntField(it, "retCode")
                val errCode = XposedHelpers.getIntField(it, "errCode")
                val values = ContentValues()
                //values.put("uin", account)
                values.put("seq", seq)
                values.put("ret", retCode)
                values.put("err", errCode)
                values.put("cmd", cmd)
                values.put("cmdId", cmdId)
                values.put("data", respData)
                values.put("extend_info", exData)
                values.put("mode", "bdh.recv")
                HookUtil.sendTo(defaultUri, values, source)
            }
        }

    }

    private fun hookParams() {
        val dandelionClz = load("com.tencent.dandelionsdk.Dandelion")
        dandelionClz.hookMethod("fly")?.after {
            val result = HookUtil.castToBytes(it.result)
            val data = HookUtil.castToBytes(it.args[0])
            val appId = XposedHelpers.getObjectField(it.thisObject, "mAppID") as? String
            val devId = XposedHelpers.getObjectField(it.thisObject, "mDevID") as? String
            HookUtil.postTo("callToken", JsonObject().apply {
                addProperty("type", "secSign")
                addProperty("data", data.toHexString())
                addProperty("result", result.toHexString())
                addProperty("appId", appId)
                addProperty("devId", devId)
            }, source)
        }

        CodecWarpper.hookMethod("onReceData")?.before {
            val data = HookUtil.castToBytes(it.args[0])
            val size = it.args[0] as Int
            HookUtil.postTo("receData", JsonObject().apply {
                addProperty("data", data.toHexString())
                addProperty("size", size)
            }, source)
        }
    }

    private fun hookLog() {
        if (ConfigPusher[KEY_OPEN_LOG] != "yes") return
        util.hookMethod("LOGD")?.after {
            val logBuilder = StringBuilder()
            logBuilder.append(it.args[0])
            repeat(it.args.size - 1) { i ->
                logBuilder.append(":")
                logBuilder.append(it.args[i + 1])
            }
            val log = logBuilder.toString()
            HookUtil.postTo("wlogin_sdk_log", JsonObject().apply {
                addProperty("type", "d")
                addProperty("value", log)
            }, source)
            DataPutter.put(DataKind.WTLOGIN_LOG, "[d] $log")
        }
        util.hookMethod("LOGI")?.after {
            val logBuilder = StringBuilder()
            logBuilder.append(it.args[0])
            repeat(it.args.size - 1) { i ->
                logBuilder.append(":")
                logBuilder.append(it.args[i + 1])
            }
            val log = logBuilder.toString()
            HookUtil.postTo("wlogin_sdk_log", JsonObject().apply {
                addProperty("type", "i")
                addProperty("value", log)
            }, source)
            DataPutter.put(DataKind.WTLOGIN_LOG, "[i] $log")
        }
        util.hookMethod("LOGW")?.after {
            val logBuilder = StringBuilder()
            logBuilder.append(it.args[0])
            repeat(it.args.size - 1) { i ->
                logBuilder.append(":")
                logBuilder.append(it.args[i + 1])
            }
            val log = logBuilder.toString()
            HookUtil.postTo("wlogin_sdk_log", JsonObject().apply {
                addProperty("type", "w")
                addProperty("value", log)
            }, source)
            DataPutter.put(DataKind.WTLOGIN_LOG, "[w] $log")
        }

        val qlog = load("com.tencent.qphone.base.util.QLog")!!
        val fieldName = arrayOf("qlog")
        qlog.hookMethod("init")?.after {
            fieldName[0] = qlog.getDeclaredField("processName").get(null) as? String ?: "qlog"
            qlog.getDeclaredMethod("setDebugMode").invoke(null, true)
        }
        XposedHelpers.findAndHookMethod(
            qlog,
            "e",
            String::class.java,
            Integer.TYPE,
            String::class.java,
            Throwable::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val tag = param.args[0]
                    val ctx = param.args[2]
                    val th = param.args[3] as Throwable?
                    val thStr = th?.stackTraceToString() ?: ""

                    HookUtil.postTo("qlog", JsonObject().apply {
                        addProperty("type", "e")
                        addProperty("tag", tag.toString())
                        addProperty("value", ctx.toString())
                        addProperty("th", thStr)
                    }, source)

                    DataPutter.put(DataKind.QLOG, "[e] $tag: $ctx$thStr")
                }
            })

        XposedHelpers.findAndHookMethod(
            qlog,
            "w",
            String::class.java,
            Integer.TYPE,
            String::class.java,
            Throwable::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val tag = param.args[0]
                    val ctx = param.args[2]
                    val th = param.args[3] as Throwable?
                    val thStr = th?.stackTraceToString() ?: ""

                    HookUtil.postTo("qlog", JsonObject().apply {
                        addProperty("type", "w")
                        addProperty("tag", tag.toString())
                        addProperty("value", ctx.toString())
                        addProperty("th", thStr)
                    }, source)

                    DataPutter.put(DataKind.QLOG, "[w] $tag: $ctx$thStr")
                }
            })

        XposedHelpers.findAndHookMethod(
            qlog,
            "i",
            String::class.java,
            Integer.TYPE,
            String::class.java,
            Throwable::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val tag = param.args[0]
                    val ctx = param.args[2]
                    val th = param.args[3] as Throwable?
                    val thStr = th?.stackTraceToString() ?: ""

                    HookUtil.postTo("qlog", JsonObject().apply {
                        addProperty("type", "i")
                        addProperty("tag", tag.toString())
                        addProperty("value", ctx.toString())
                        addProperty("th", thStr)
                    }, source)

                    DataPutter.put(DataKind.QLOG, "[i] $tag: $ctx$thStr")
                }
            })

        XposedHelpers.findAndHookMethod(
            qlog,
            "d",
            String::class.java,
            Integer.TYPE,
            String::class.java,
            Throwable::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val tag = param.args[0]
                    val ctx = param.args[2]
                    val th = param.args[3] as Throwable?
                    val thStr = th?.stackTraceToString() ?: ""

                    HookUtil.postTo("qlog", JsonObject().apply {
                        addProperty("type", "d")
                        addProperty("tag", tag.toString())
                        addProperty("value", ctx.toString())
                        addProperty("th", thStr)
                    }, source)

                    DataPutter.put(DataKind.QLOG, "[d] $tag: $ctx$thStr")
                }
            })
        qlog.hookMethod("isColorLevel")?.after {
            it.result = true
        }
        qlog.hookMethod("isDevelopLevel")?.after {
            it.result = true
        }
        qlog.hookMethod("isDebugVersion")?.after {
            it.result = true
        }
        qlog.hookMethod("setFullEncryptedLogMode")?.before {
            it.args[0] = false
        }
        qlog.hookMethod("isEncrypted")?.after {
            it.result = false
        }
    }

    private fun hookMD5() {
        XposedHelpers.findAndHookMethod(
            MD5,
            "toMD5Byte",
            ByteArray::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val data = HookUtil.castToBytes(param.args[0])
                    val result = param.result as ByteArray? ?: EMPTY_BYTE_ARRAY
                    submitMd5(data, result)
                }
            })
        XposedHelpers.findAndHookMethod(
            MD5,
            "toMD5Byte",
            String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val data = (param.args[0] as String?)?.toByteArray()
                    if (data != null) {
                        val result = param.result as ByteArray
                        submitMd5(data, result)
                    }
                }
            })

        XposedHelpers.findAndHookMethod(MD5, "toMD5", String::class.java, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val data = (param.args[0] as String?)?.toByteArray()
                if (data != null) {
                    val result = (param.result as String).hex2ByteArray()
                    submitMd5(data, result)
                }
            }
        })
        XposedHelpers.findAndHookMethod(
            MD5,
            "toMD5",
            ByteArray::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val data = HookUtil.castToBytes(param.args[0])
                    val result = (param.result as String).hex2ByteArray()
                    submitMd5(data, result)
                }
            })
    }

    private fun submitMd5(data: ByteArray, result: ByteArray) {
        val value = ContentValues()
        value.put("mode", "md5")
        value.put("data", data)
        value.put("result", result)
        HookUtil.sendTo(defaultUri, value, source)
    }

    private fun hookTlv() {
        kotlin.runCatching {
            val cmd = tlv_t!!.getDeclaredField("_cmd").also {
                it.isAccessible = true
            }
            tlv_t.hookMethod("get_buf")?.after {
                val thiz = it.thisObject
                val result = it.result as ByteArray
                val tlvVer = cmd.get(thiz) as Int

                val value = ContentValues()
                value.put("mode", "tlv.get_buf")
                value.put("data", result)
                value.put("version", tlvVer)
                HookUtil.sendTo(defaultUri, value, source)
            }

            val buf = tlv_t.getDeclaredField("_buf").also {
                it.isAccessible = true
            }
            tlv_t.hookMethod("get_tlv")?.after {
                val thiz = it.thisObject
                val result = buf.get(thiz) as ByteArray
                val tlvVer = cmd.get(thiz) as Int

                val value = ContentValues()
                value.put("mode", "tlv.set_buf")
                value.put("data", result)
                value.put("version", tlvVer)
                HookUtil.sendTo(defaultUri, value, source)
            }
        }
    }

    // cache 0xff_id
    private fun hookEcdh(
        oicq_request: Class<*>,
        EcdhCrypt: Class<*>,
        WtloginHelper: Class<*>,
        ctx: Context
    ) {
        if (ConfigPusher[KEY_ECDH_DEFAULT] == "yes") {
            // log("强制使用默认密钥开启")
            EcdhCrypt.hookMethod("initShareKey")?.before {
                val field = EcdhCrypt.getDeclaredField("userOpenSSLLib")
                if (!field.isAccessible)
                    field.isAccessible = true
                field.set(null, false)

                callMethod(it.thisObject, "initShareKeyByDefault")
                it.result = null
            }
        } // 强制使用默认ecdh

        if (ConfigPusher[KEY_ECDH_NEW_HOOK] != "no") {
            if (GlobalData != null) {
                // lateinit var GlobalDataObject: Any 0xff01
                // lateinit var possibleFields: Array<Field> 0xff02
                WtloginHelper.hookMethod("ShareKeyInit")?.before {
                    GlobalCache[0xff01] = it.thisObject.javaClass.declaredFields
                        .first { it.type == GlobalData }
                        .also { if (!it.isAccessible) it.isAccessible = true }
                        .get(it.thisObject)!!

                    if (!globalDataFields.containsKey(1) || !globalDataFields.containsKey(0)) {
                        GlobalCache[0xff02] = GlobalData!!.fields.filter {
                            it.type == ByteArray::class.java
                                    && (XposedHelpers.getObjectField(
                                GlobalCache[0xff01],
                                it.name
                            ) as? ByteArray).contentHashCode() == 1353309697
                        }.toTypedArray()
                    }
                }?.after {
                    val GlobalDataObject = GlobalCache[0xff01]
                    // val dataMap: HashMap<Int, ByteArray> = hashMapOf()
                    if (!globalDataFields.containsKey(1) || !globalDataFields.containsKey(0)) {
                        (GlobalCache[0xff02] as Array<Field>).forEach {
                            (XposedHelpers.getObjectField(
                                GlobalDataObject,
                                it.name
                            ) as? ByteArray).let { data ->
                                if (data != null && data.contentHashCode() != 1353309697) {
                                    globalDataFields[if (data.size > 16) 0 else 1] = it.name
                                    // 0 public 1 share
                                }
                            }
                        }
                        // GlobalCache.remove(0xff01) 不释放 后面要用的
                        GlobalCache.remove(0xff02) // 释放引用
                    }
                    ((XposedHelpers.getObjectField(
                        GlobalDataObject,
                        globalDataFields[0]
                    ) as? ByteArray)!! to
                            (XposedHelpers.getObjectField(
                                GlobalDataObject,
                                globalDataFields[1]
                            ) as? ByteArray)!!).let {
                        if (it.first.contentHashCode() != 1353309697) {
                            val hexPub = it.first.toHexString()
                            val hexShr = it.second.toHexString()
                            DataPutter.put(DataKind.ECDH_SHARE, hexShr)
                            DataPutter.put(DataKind.ECDH_PUBLIC, hexPub)
                        }
                    }
                    ConfigPusher.getData(KEY_DATA_PUBLIC).let {
                        if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
                    }.let {
                        val token = it[source]
                        if (token.isLock) {
                            //Toast.toast(ctx, "检测到ECDH被发生变化")
                            XposedHelpers.setObjectField(
                                GlobalDataObject,
                                globalDataFields[0],
                                token.token
                            )
                        }
                    }

                    ConfigPusher.getData(KEY_DATA_SHARE).let {
                        if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
                    }.let {
                        val token = it[source]
                        if (token.isLock) {
                            XposedHelpers.setObjectField(
                                GlobalDataObject,
                                globalDataFields[1],
                                token.token
                            )
                            //Toast.toast(ctx, "强制固定ECDH成功")
                        }
                    }
                }
            }


            // log("正在寻找新Ecdh代码点...")
            oicq_request.declaredMethods.filter {
                // if (it.name == "a") log(it.toGenericString())
                val params = it.parameterTypes
                params.size == 5 &&
                        params[0] == ByteArray::class.java && // data
                        params[1] == ByteArray::class.java && // random
                        params[2] == ByteArray::class.java && // public key
                        params[3] == ByteArray::class.java && // share key
                        params[4] == Int::class.java
            }.forEach {
                // log("载入新EcdhHooker: $it")
                XposedHelpers.findAndHookMethod(oicq_request,
                    it.name,
                    ByteArray::class.java,
                    ByteArray::class.java,
                    ByteArray::class.java,
                    ByteArray::class.java,
                    Int::class.java,
                    object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val share = HookUtil.castToBytes(param.args[3]).clone()
                            val public = HookUtil.castToBytes(param.args[2]).clone()
                            if (hasStorePermission(ctx)) {
                                ConfigPusher.setData(KEY_DATA_PUBLIC, ProtoBuf.encodeToByteArray(
                                    ConfigPusher.getData(KEY_DATA_PUBLIC).let {
                                        if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
                                    }.also {
                                        it[source].also {
                                            if (it.isLock) {
                                                param.args[2] = it.token
                                                //DataPutter.put(DataKind.ECDH_PUBLIC, "[FIX]" + it.token.toHexString())
                                            } else {
                                                it.token = public
                                            }
                                        }
                                    }
                                ))
                                ConfigPusher.setData(KEY_DATA_SHARE, ProtoBuf.encodeToByteArray(
                                    ConfigPusher.getData(KEY_DATA_SHARE).let {
                                        if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
                                    }.also {
                                        it[source].also {
                                            if (it.isLock) {
                                                param.args[3] = it.token
                                                //DataPutter.put(DataKind.ECDH_SHARE, "[FIX]" + it.token.toHexString())
                                            } else {
                                                it.token = share
                                            }
                                        }
                                    }
                                ))
                            } else {
                                HookUtil.sendTo(defaultUri, ContentValues().apply {
                                    put("mode", "ecdh.c_pub_key")
                                    put("data", public)
                                }, source)
                                HookUtil.sendTo(defaultUri, ContentValues().apply {
                                    put("mode", "ecdh.g_share_key")
                                    put("data", share)
                                }, source)
                            }
                            // EcdhFucker.setShareKey(source, share)
                            // EcdhFucker.setPublicKey(source, public)
                        }
                    }
                )
            }
        } else {
            EcdhCrypt.hookMethod("set_c_pub_key", ByteArray::class.java)?.before {
                val bytes = HookUtil.castToBytes(it.args[0])

                if (bytes.isNotEmpty()) {
                    if (hasStorePermission(ctx)) {
                        ConfigPusher.setData(KEY_DATA_PUBLIC, ProtoBuf.encodeToByteArray(
                            ConfigPusher.getData(KEY_DATA_PUBLIC).let {
                                if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
                            }.also { sv ->
                                sv[source].also { tk ->
                                    if (!tk.isLock) {
                                        tk.token = bytes
                                    } else {
                                        it.args[0] = tk.token
                                    }
                                }
                            }
                        ))
                    } else {
                        HookUtil.sendTo(defaultUri, ContentValues().apply {
                            put("mode", "ecdh.c_pub_key")
                            put("data", bytes)
                        }, source)
                    }
                }
            }

            EcdhCrypt.hookMethod("set_g_share_key")?.before {
                val bytes = HookUtil.castToBytes(it.args[0])
                if (bytes.isNotEmpty()) {
                    if (hasStorePermission(ctx)) {
                        ConfigPusher.setData(KEY_DATA_SHARE, ProtoBuf.encodeToByteArray(
                            ConfigPusher.getData(KEY_DATA_SHARE).let {
                                if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
                            }.also { sv ->
                                sv[source].also { tk ->
                                    if (!tk.isLock) {
                                        tk.token = bytes
                                    } else {
                                        it.args[0] = tk.token
                                    }
                                }
                            }
                        ))
                    } else {
                        HookUtil.sendTo(defaultUri, ContentValues().apply {
                            put("mode", "ecdh.g_share_key")
                            put("data", bytes)
                        }, source)
                    }
                }
                //log("拿到啦1！！！！！！！！！")
            }

            EcdhCrypt.hookMethod("get_c_pub_key")?.after {
                val bytes = HookUtil.castToBytes(it.result)

                if (bytes.isNotEmpty()) {
                    if (hasStorePermission(ctx)) {
                        val saved = ConfigPusher.getData(KEY_DATA_PUBLIC).let {
                            if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
                        }
                        saved[source].let {
                            if (!it.isLock) it.token = bytes
                        }
                        ConfigPusher.setData(KEY_DATA_PUBLIC, ProtoBuf.encodeToByteArray(saved))
                        val hexKey = bytes.toHexString()
                        HookUtil.postTo("ecdh", JsonObject().apply {
                            addProperty("type", "public")
                            addProperty("key", hexKey)
                        }, source)
                        DataPutter.put(DataKind.ECDH_PUBLIC, hexKey)
                        // EcdhFucker.setPublicKey(source, bytes)
                    } else {
                        HookUtil.sendTo(defaultUri, ContentValues().apply {
                            put("mode", "ecdh.c_pub_key")
                            put("data", bytes)
                        }, source)
                    }
                }
            }

            EcdhCrypt.hookMethod("get_g_share_key")?.after {
                val bytes = HookUtil.castToBytes(it.result)

                if (bytes.isNotEmpty()) {
                    if (hasStorePermission(ctx)) {
                        val saved = ConfigPusher.getData(KEY_DATA_SHARE).let {
                            if (it != null) ProtoBuf.decodeFromByteArray(it) else SavedToken()
                        }
                        saved[source].let {
                            if (!it.isLock) it.token = bytes
                        }
                        ConfigPusher.setData(KEY_DATA_SHARE, ProtoBuf.encodeToByteArray(saved))
                        val hexKey = bytes.toHexString()
                        HookUtil.postTo("ecdh", JsonObject().apply {
                            addProperty("type", "share")
                            addProperty("key", hexKey)
                        }, source)
                        DataPutter.put(DataKind.ECDH_SHARE, hexKey)
                    } else {
                        HookUtil.sendTo(defaultUri, ContentValues().apply {
                            put("mode", "ecdh.g_share_key")
                            put("data", bytes)
                        }, source)
                    }
                }
            }

            EcdhCrypt.hookMethod("set_c_pri_key")?.before {
                val bytes = HookUtil.castToBytes(it.args[0])
                if (bytes.isNotEmpty()) {
                    val hexKey = bytes.toHexString()
                    HookUtil.postTo("ecdh", JsonObject().apply {
                        addProperty("type", "pri")
                        addProperty("key", hexKey)
                    }, source)
                }
            }
        }
    }

    private fun hookTea() {
        cryptor.hookMethod("encrypt")?.after {
            val value = ContentValues()
            value.put("enc", true)
            value.put("mode", "tea")
            value.put("data", HookUtil.castToBytes(it.args[0]))
            value.put("result", HookUtil.castToBytes(it.result))
            value.put("key", HookUtil.castToBytes(it.args[3]))
            HookUtil.sendTo(defaultUri, value, source)
        }
        cryptor.hookMethod("decrypt")?.after {
            val value = ContentValues()
            value.put("enc", false)
            value.put("mode", "tea")
            value.put("data", HookUtil.castToBytes(it.args[0]))
            value.put("result", HookUtil.castToBytes(it.result))
            value.put("key", HookUtil.castToBytes(it.args[3]))
            HookUtil.sendTo(defaultUri, value, source)
        }
    }

    private fun hookSendPacket() {
        CodecWarpper.hookMethod("encodeRequest")?.after { param ->
            val args = param.args
            when (args.size) {
                17 -> {
                    val seq = args[0] as Int
                    val cmd = args[5] as String
                    // -- qimei [15] imei [2] version [4]

                    if (!isIgnore(cmd)) {
                        val msgCookie = args[6] as? ByteArray
                        val uin = args[9] as String
                        val buffer = args[15] as ByteArray
                        val util = ContentValues()

                        util.put("uin", uin)
                        util.put("seq", seq)

                        util.put("cmd", cmd)
                        util.put("type", "unknown")
                        util.put("msgCookie", msgCookie ?: EMPTY_BYTE_ARRAY)
                        util.put("buffer", buffer)

                        util.put("mode", "send")

                        HookUtil.sendTo(defaultUri, util, source)
                    }
                }
                14 -> {
                    val seq = args[0] as Int
                    val cmd = args[5] as String
                    if (!isIgnore(cmd)) {
                        val msgCookie = args[6] as? ByteArray
                        val uin = args[9] as String
                        val buffer = args[12] as ByteArray

                        val util = ContentValues()
                        util.put("uin", uin)
                        util.put("seq", seq)
                        util.put("cmd", cmd)
                        util.put("type", "unknown")
                        util.put("msgCookie", msgCookie ?: EMPTY_BYTE_ARRAY)
                        util.put("buffer", buffer)

                        util.put("mode", "send")

                        HookUtil.sendTo(defaultUri, util, source)
                    }
                }
                16 -> {
                    val seq = args[0] as Int
                    val cmd = args[5] as String
                    if (!isIgnore(cmd)) {
                        val msgCookie = args[6] as? ByteArray
                        val uin = args[9] as String
                        val buffer = args[14] as ByteArray
                        // -- qimei [15] imei [2] version [4]

                        val util = ContentValues()
                        util.put("uin", uin)
                        util.put("seq", seq)
                        util.put("cmd", cmd)
                        util.put("type", "unknown")
                        util.put("msgCookie", msgCookie ?: EMPTY_BYTE_ARRAY)
                        util.put("buffer", buffer)

                        util.put("mode", "send")

                        HookUtil.sendTo(defaultUri, util, source)
                    }
                }
                else -> {
                    log("hook到了个不知道什么东西")
                }
            }
        }
    }

    private fun hookReceive(
        clazz: Class<*>
    ) {
        clazz.hookMethod("onResponse")?.after { param ->
            val from = param.args[1]
            val seq = HookUtil.invokeFromObjectMethod(from, "getRequestSsoSeq") as Int
            val cmd = HookUtil.invokeFromObjectMethod(from, "getServiceCmd") as String
            if (!isIgnore(cmd)) {
                val msgCookie = HookUtil.invokeFromObjectMethod(from, "getMsgCookie") as? ByteArray
                val uin = HookUtil.invokeFromObjectMethod(from, "getUin") as String
                val buffer = HookUtil.invokeFromObjectMethod(from, "getWupBuffer") as ByteArray
                // -- qimei [15] imei [2] version [4]

                val util = ContentValues()
                util.put("uin", uin)
                util.put("seq", seq)
                util.put("cmd", cmd)
                util.put("type", "unknown")
                util.put("msgCookie", msgCookie ?: EMPTY_BYTE_ARRAY)
                util.put("buffer", buffer)

                util.put("mode", "receive")

                HookUtil.sendTo(defaultUri, util, source)
            }
        }
    }
}

class HookClassloader(
    private val qqloader: ClassLoader
) : ClassLoader() {
    override fun loadClass(name: String?, resolve: Boolean): Class<*> {
        try {
            return sBootClassLoader.loadClass(name)
        } catch (e: ClassNotFoundException) {
        }
        try {
            return qqloader.loadClass(name)
        } catch (e: ClassNotFoundException) {
        }
        return getSystemClassLoader().loadClass(name)
    }

    companion object {
        private val sBootClassLoader: ClassLoader = MainHook::class.java.classLoader!!
    }
}