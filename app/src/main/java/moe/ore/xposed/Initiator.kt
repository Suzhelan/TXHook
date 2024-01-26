package moe.ore.xposed

import moe.ore.xposed.HookEntry.Companion.PACKAGE_NAME_QQ

object Initiator {
    private var sHostClassLoader: ClassLoader? = null
    private var sPluginParentClassLoader: ClassLoader? = null

    fun init(classLoader: ClassLoader?) {
        sHostClassLoader = classLoader
        sPluginParentClassLoader = Initiator::class.java.classLoader
    }

    fun getPluginClassLoader(): ClassLoader? {
        return Initiator::class.java.classLoader
    }

    fun getHostClassLoader(): ClassLoader? {
        return sHostClassLoader
    }

    fun load(className: String?): Class<*>? {
        var className = className
        if (sPluginParentClassLoader == null || className == null || className.isEmpty()) {
            return null
        }
        className = className.replace('/', '.')
        if (className.endsWith(";")) {
            className = if (className[0] == 'L') {
                className.substring(1, className.length - 1)
            } else {
                className.substring(0, className.length - 1)
            }
        }
        if (className.startsWith(".")) {
            className = PACKAGE_NAME_QQ + className
        }
        return try {
            sHostClassLoader!!.loadClass(className)
        } catch (e: Throwable) {
            null
        }
    }

}