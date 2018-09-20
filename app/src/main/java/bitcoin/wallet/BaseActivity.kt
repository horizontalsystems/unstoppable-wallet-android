package bitcoin.wallet

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import bitcoin.wallet.core.App
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.modules.pin.PinModule

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lightMode = Factory.preferencesManager.isLightModeEnabled()
        setTheme(if (lightMode) R.style.LightModeAppTheme else R.style.DarkModeAppTheme)
        if (savedInstanceState != null) {
            setStatusBarIconColor(lightMode)
        }

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onResume() {
        super.onResume()

        if (App.promptPin) {
            PinModule.startForUnlock(this)
            return
        }

        App.promptPin = false
    }

    private fun setStatusBarIconColor(lightMode: Boolean) {
        var flags = window.decorView.systemUiVisibility
        flags = if (lightMode) {
            flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            flags xor View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR // remove flag
        }
        window.decorView.systemUiVisibility = flags
    }

}
