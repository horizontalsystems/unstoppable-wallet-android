package bitcoin.wallet

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.security.keystore.UserNotAuthenticatedException
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import bitcoin.wallet.core.App
import bitcoin.wallet.core.managers.Factory
import bitcoin.wallet.core.security.EncryptionManager
import bitcoin.wallet.modules.pin.PinModule

abstract class BaseActivity : AppCompatActivity() {

    protected open var requiresPinUnlock = true
    private var pendingRunnable: Runnable? = null
    private var successRunnable: Runnable? = null
    private var failureRunnable: Runnable? = null

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
        if (requiresPinUnlock) {
            safeExecuteWithKeystore(
                    action = Runnable {
                        if (App.promptPin && Factory.preferencesManager.getPin() != null) {
                            PinModule.startForUnlock(this)
                        }
                    },
                    onSuccess = Runnable { App.promptPin = false },
                    onFailure = null
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTHENTICATE_FOR_ENCRYPTION) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    pendingRunnable?.run()
                    successRunnable?.run()
                } catch (e: Exception){
                    failureRunnable?.run()
                }
            } else {
                failureRunnable?.run()
            }
            pendingRunnable = null
            failureRunnable = null
            successRunnable = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun safeExecuteWithKeystore(action: Runnable, onSuccess: Runnable?, onFailure: Runnable?) {
        try {
            action.run()
            onSuccess?.run()
        } catch (e: UserNotAuthenticatedException) {
            pendingRunnable = action
            successRunnable = onSuccess
            failureRunnable = onFailure
            EncryptionManager.showAuthenticationScreen(this, AUTHENTICATE_FOR_ENCRYPTION)
        } catch (e: Exception) {
            onFailure?.run()
        }
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

    companion object {
        const val AUTHENTICATE_FOR_ENCRYPTION = 23
    }

}
