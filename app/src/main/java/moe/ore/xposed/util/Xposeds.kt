package moe.ore.xposed.util

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedBridge.log
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XCallback

fun callMethod(obj: Any?, funName: String?, vararg args: Any?): Any? {
    return XposedHelpers.callMethod(obj, funName, *args)
}

/*
fun <T> Any.get(name: String): T? {
    return javaClass.getField(name).get(this) as? T
}

fun Any.set(name: String, value: Any?, obj: Any? = null) {
    javaClass.getField(name).set(obj, value)
}*/

fun Class<*>?.callStaticMethod(funName: String?, vararg args: Any?): Any? {
    return XposedHelpers.callStaticMethod(this, funName, *args)
}

fun Class<*>?.hookMethod(funName: String?): XposedMethodHook? {
    return try {
        val hook = XposedMethodHook()

        XposedBridge.hookAllMethods(this, funName, hook)

        hook
    } catch (e: Exception) {
        log(e)
        null
    }
}

fun Class<*>?.hookMethod(funName: String?, vararg args: Class<*>): XposedMethodHook? {
    return try {
        val hook = XposedMethodHook()

        val anise = arrayOfNulls<Any>(args.size + 1)
        args.forEachIndexed { index, clazz ->
            anise[index] = clazz
        }
        anise[anise.size - 1] = hook

        XposedHelpers.findAndHookMethod(this, funName, *anise)

        hook
    } catch (e: Exception) {
        log(e)
        e.printStackTrace()
        null
    }
}

fun hookMethod(
    clz: String?,
    loader: ClassLoader?,
    funName: String?,
    vararg args: Class<*>
): XposedMethodHook? {
    return try {
        val hook = XposedMethodHook()

        val anise = arrayOfNulls<Any>(args.size + 1)
        args.forEachIndexed { index, clazz ->
            anise[index] = clazz
        }
        anise[anise.size - 1] = hook

        XposedHelpers.findAndHookMethod(clz, loader, funName, *anise)

        hook
    } catch (e: Exception) {
        null
    }
}

fun beforeHook(block: (param: XC_MethodHook.MethodHookParam) -> Unit): XC_MethodHook {
    return object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            block(param)
        }
    }
}

fun afterHook(
    ver: Int = XCallback.PRIORITY_DEFAULT,
    block: (param: XC_MethodHook.MethodHookParam) -> Unit
): XC_MethodHook {
    return object : XC_MethodHook(ver) {
        override fun afterHookedMethod(param: MethodHookParam) {
            block(param)
        }
    }
}

class XposedMethodHook : XC_MethodHook() {
    private lateinit var beforeFun: XposedMethodHookFunction
    private lateinit var afterFun: XposedMethodHookFunction

    fun before(function: XposedMethodHookFunction): XposedMethodHook {
        this.beforeFun = function
        return this
    }

    fun after(function: XposedMethodHookFunction): XposedMethodHook {
        this.afterFun = function
        return this
    }

    override fun beforeHookedMethod(param: MethodHookParam) {
        if (this::beforeFun.isInitialized) {
            try {
                beforeFun(param)
            } catch (e: Exception) {
                if (!e.message.contentEquals("content://moe.ore")) log(e)
            }
        }
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        if (this::afterFun.isInitialized) {
            try {
                afterFun(param)
            } catch (e: Exception) {
                if (!e.message.contentEquals("content://moe.ore")) log(e)
            }
        }
    }
}

fun interface XposedMethodHookFunction {
    operator fun invoke(param: XC_MethodHook.MethodHookParam)
}

