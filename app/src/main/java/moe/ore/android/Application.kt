package moe.ore.android

import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import java.util.Locale

class Application : android.app.Application() {
    init {
        weakSelf = WeakReference(this)
    }


    override fun onCreate() {
        super.onCreate()

        locale = Locale.getDefault()
        defaultLocale = Locale.getDefault()

        AndroKtx.init(this)
    }

    companion object {
        val uiHandler: Handler = Handler(Looper.getMainLooper())

        lateinit var weakSelf: WeakReference<Application>
        lateinit var locale: Locale
        lateinit var defaultLocale: Locale
    }
}