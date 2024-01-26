package moe.ore.script

import android.content.Context
import moe.ore.android.toast.Toast
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class AndroPrint(
    val isErr: Boolean,
    val context: Context,
    val stream: ByteArrayOutputStream = ByteArrayOutputStream()
) : PrintStream(stream, true) {
    override fun print(b: Boolean) {
        super.print(b)
        Toast.toast(context, if (b) "true" else "false")
    }

    override fun print(c: Char) {
        super.print(c)
        Toast.toast(context, c.toString())
    }

    override fun print(i: Int) {
        super.print(i)
        Toast.toast(context, i.toString())
    }

    override fun print(l: Long) {
        super.print(l)
        Toast.toast(context, l.toString())
    }

    override fun print(f: Float) {
        super.print(f)
        Toast.toast(context, f.toString())
    }

    override fun print(d: Double) {
        super.print(d)
        Toast.toast(context, d.toString())
    }

    override fun print(s: CharArray?) {
        super.print(s)
        Toast.toast(context, String(s ?: charArrayOf()))
    }

    override fun print(s: String?) {
        super.print(s)
        Toast.toast(context, s.toString())
    }

    override fun print(obj: Any) {
        super.print(obj)
        Toast.toast(context, obj.toString())
    }
}