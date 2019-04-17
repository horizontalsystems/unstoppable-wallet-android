package io.horizontalsystems.bankwallet

import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.UserNotAuthenticatedException
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.security.EncryptionManager
import io.horizontalsystems.bankwallet.lib.AlertDialogFragment
import java.util.*

abstract class BaseActivity : AppCompatActivity() {

    private var failureRunnable: Runnable? = null
    private var actionRunnable: Runnable? = null
    private var successRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lightMode = App.localStorage.isLightModeOn
        setTheme(if (lightMode) R.style.LightModeAppTheme else R.style.DarkModeAppTheme)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == AUTHENTICATE_FOR_ENCRYPTION) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    actionRunnable?.run()
                    successRunnable?.run()
                } catch (e: Exception) {
                    failureRunnable?.run()
                }
            } else {
                failureRunnable?.run()
            }
            failureRunnable = null
            actionRunnable = null
            successRunnable = null
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    fun safeExecuteWithKeystore(action: Runnable, onSuccess: Runnable? = null, onFailure: Runnable? = null) {
        try {
            action.run()
            onSuccess?.run()
        } catch (e: UserNotAuthenticatedException) {
            actionRunnable = action
            successRunnable = onSuccess
            failureRunnable = onFailure
            EncryptionManager.showAuthenticationScreen(this, AUTHENTICATE_FOR_ENCRYPTION)
        } catch (e: KeyPermanentlyInvalidatedException) {
            EncryptionManager.showKeysInvalidatedAlert(this)
        } catch (e: Exception) {
            onFailure?.run()
        }

    }

    override fun attachBaseContext(newBase: Context?) {
        newBase?.let {
            super.attachBaseContext(updateBaseContextLocale(it))
        } ?: super.attachBaseContext(newBase)
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        if (App.appConfigProvider.testMode) {
            showTestLabel()
        }
    }

    protected fun hideSoftKeyboard() {
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    protected fun setTransparentStatusBar() {
        window.statusBarColor = Color.TRANSPARENT
    }

    fun showCustomKeyboardAlert() {
        AlertDialogFragment.newInstance(R.string.Alert_TitleWarning, R.string.Alert_CustomKeyboardIsUsed, R.string.Alert_Ok,
                object : AlertDialogFragment.Listener {
                    override fun onButtonClick() {
                        val imeManager = App.instance.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imeManager.showInputMethodPicker()

                        Handler().postDelayed({
                            onBackPressed()
                        }, (1 * 750).toLong())
                    }
                }).show(supportFragmentManager, "custom_keyboard_alert")
    }

    private fun showTestLabel() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val testLabelTv = TextView(this)
        testLabelTv.text = "Test"
        testLabelTv.setPadding(5, 3, 5, 3)
        testLabelTv.includeFontPadding = false
        testLabelTv.setBackgroundColor(Color.RED)
        testLabelTv.setTextColor(Color.WHITE)
        testLabelTv.textSize = 12f
        val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL
        testLabelTv.layoutParams = layoutParams
        rootView.addView(testLabelTv)
    }

    private fun updateBaseContextLocale(context: Context): Context {
        val language = App.languageManager.currentLanguage
        Locale.setDefault(language)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResourcesLocale(context, language)
        } else updateResourcesLocaleLegacy(context, language)
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun updateResourcesLocale(context: Context, locale: Locale): Context {
        val configuration: Configuration = context.resources.configuration
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    @SuppressWarnings("deprecation")
    private fun updateResourcesLocaleLegacy(context: Context, locale: Locale): Context {
        val resources: Resources = context.resources
        val configuration: Configuration = resources.configuration
        configuration.locale = locale
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }

    companion object {
        const val AUTHENTICATE_FOR_ENCRYPTION = 23
    }

}
