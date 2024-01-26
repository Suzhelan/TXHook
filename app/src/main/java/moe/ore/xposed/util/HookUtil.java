package moe.ore.xposed.util;

import static moe.ore.xposed.helper.ConfigPusher.KEY_PUSH_API;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;

import de.robv.android.xposed.XposedBridge;
import moe.ore.android.util.HttpUtil;
import moe.ore.script.Consist;
import moe.ore.txhook.app.CatchProvider;
import moe.ore.txhook.helper.HexUtil;
import moe.ore.txhook.helper.KotlinExtKt;
import moe.ore.txhook.helper.ThreadManager;
import moe.ore.xposed.helper.ConfigPusher;
import moe.ore.xposed.ws.ArkWebSocket;

public final class HookUtil {
    private static final Uri URI_GET_TXHOOK_STATE = Uri.parse("content://" + CatchProvider.MY_URI + "/" + Consist.GET_TXHOOK_STATE);
    public static WeakReference<Context> contextWeakReference;
    // private static final Uri URI_GET_TXHOOK_WS_STATE = Uri.parse("content://" + CatchProvider.MY_URI + "/" + Consist.GET_TXHOOK_WS_STATE);
    public static ContentResolver contentResolver;
    private static ArkWebSocket webSocket = null;

    public static void sendTo(Uri uri, ContentValues contentValues, int source) {
        try {
            String url = getApiUrl();
            String mode = contentValues.getAsString("mode");
            if (url != null && !url.isEmpty()) {
                String name;
                switch (mode) {
                    case "send":
                    case "receive":
                        name = "packet";
                        break;
                    default:
                        name = mode;
                        break;
                }
                JsonObject object = new JsonObject();
                for (String key : contentValues.keySet()) {
                    Object value = contentValues.get(key);
                    if (value == null) continue;
                    if (value instanceof Number) {
                        object.addProperty(key, (Number) value);
                    } else if (value instanceof String) {
                        object.addProperty(key, (String) value);
                    } else if (value instanceof Boolean) {
                        object.addProperty(key, (Boolean) value);
                    } else if (value instanceof byte[]) {
                        object.addProperty(key, HexUtil.Bin2Hex((byte[]) value));
                    }
                }
                postTo(url, name, object, source);
            }

            // tryToConnectWS();
            if ("running".equals(contentResolver.getType(URI_GET_TXHOOK_STATE))) {
                contentValues.put("source", source);
                contentResolver.insert(uri, contentValues);
                // 如果查询不到状态就不发了
            }
        } catch (IllegalArgumentException e) {

        }
    }

    public static void tryToConnectWS(String url, int source) {
        //String url = resolver.getType(URI_GET_TXHOOK_WS_STATE);
        if (url == null || TextUtils.isEmpty(url) || !url.startsWith("ws")) return;
        if (webSocket != null && webSocket.isConnected()) return;
        webSocket = new ArkWebSocket(url);
        ThreadManager.getInstance(0).addTask(new Thread() {
            @Override
            public void run() {
                try {
                    XposedBridge.log("尝试连接WS：" + url);
                    webSocket.open(source);
                    XposedBridge.log("连接WS成功：" + url);
                } catch (Exception e) {
                    XposedBridge.log("连接WS失败：" + Log.getStackTraceString(e));
                }
            }
        });
    }

    public static void postTo(String action, JsonObject jsonObject, int source) {
        String url = getApiUrl();
        postTo(url, action, jsonObject, source);
    }

    public static void postTo(String url, String action, JsonObject jsonObject, int source) {
        if (url != null && !url.isEmpty()) {
            jsonObject.addProperty("source", source);
            HttpUtil.INSTANCE.postJson("http://" + url + "/" + action, jsonObject.toString());
        }
    }

    public static String getApiUrl() {
        String url = ConfigPusher.INSTANCE.get(KEY_PUSH_API);
        if (contentResolver != null) {
            url = contentResolver
                    .query(URI_GET_TXHOOK_STATE, null, KEY_PUSH_API, null, null)
                    .getExtras()
                    .getString(KEY_PUSH_API);
        }
        return url;
    }

    public static Object invokeFromObjectMethod(Object from, String mn, Class<?>... parameterTypes) {
        Class<?> c = from.getClass();
        try {
            return c.getDeclaredMethod(mn, parameterTypes).invoke(from);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @NotNull
    public static byte[] castToBytes(Object result) {
        if (result == null) {
            return KotlinExtKt.EMPTY_BYTE_ARRAY;
        }
        return (byte[]) result;
    }
}
