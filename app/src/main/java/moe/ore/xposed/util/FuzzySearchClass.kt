package moe.ore.xposed.util

import moe.ore.xposed.Initiator
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

object FuzzySearchClass {
    /**
     * QQ混淆字典
     */
    private val dic = arrayOf(
        "r",
        "t",
        "a",
        "b",
        "c",
        "e",
        "f",
        "d",
        "g",
        "h",
        "i",
        "j",
        "k",
        "l",
        "m",
        "n",
        "o",
        "p",
        "q",
        "s",
        "t",
        "u",
        "v",
        "w",
        "x",
        "y",
        "z"
    )

    /**
     * 通过特殊字段寻找类
     */
    fun findClassByField(prefix: String, check: (Field) -> Boolean): Class<*>? {
        dic.forEach { className ->
            val clz = Initiator.load("$prefix.$className")
            clz?.fields?.forEach {
                if (it.modifiers and Modifier.STATIC != 0
                    && !isBaseType(it.type)
                    && check(it)
                ) return clz
            }
        }
        return null
    }

    fun findClassByMethod(prefix: String, check: (Class<*>, Method) -> Boolean): Class<*>? {
        dic.forEach { className ->
            val clz = Initiator.load("$prefix.$className")
            clz?.methods?.forEach {
                if (check(clz, it)) return clz
            }
        }
        return null
    }

    private fun isBaseType(clz: Class<*>): Boolean {
        if (
            clz == Long::class.java ||
            clz == Double::class.java ||
            clz == Float::class.java ||
            clz == Int::class.java ||
            clz == Short::class.java ||
            clz == Char::class.java ||
            clz == Byte::class.java
        ) {
            return true
        }
        return false
    }
}