package io.horizontalsystems.bankwallet

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.dialogs.AlertDialogFragment
import java.util.*
import java.util.logging.Logger

abstract class BaseActivity : AppCompatActivity() {

    private val logger = Logger.getLogger("BaseActivity")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lightMode = App.localStorage.isLightModeOn
        setTheme(if (lightMode) R.style.LightModeAppTheme else R.style.DarkModeAppTheme)

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId){
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun hideSoftKeyboard() {
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    protected fun setTransparentStatusBar() {
        val oldFlags = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = oldFlags or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    fun showCustomKeyboardAlert() {
        AlertDialogFragment.newInstance(R.string.Alert_TitleWarning, R.string.Alert_CustomKeyboardIsUsed, R.string.Alert_Ok, false,
                object : AlertDialogFragment.Listener {
                    override fun onButtonClick() {
                        val imeManager = App.instance.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imeManager.showInputMethodPicker()
                        hideSoftKeyboard()
                        Handler().postDelayed({
                            try {
                                onBackPressed()
                            } catch (e: NullPointerException) {
                                logger.warning("showCustomKeyboardAlert -> onBackPressed() caused NullPointerException")
                            }
                        }, (1 * 750).toLong())
                    }
                }).show(supportFragmentManager, "custom_keyboard_alert")
    }

    fun setTopMarginByStatusBarHeight(view: View) {
        val newLayoutParams = view.layoutParams as ConstraintLayout.LayoutParams
        newLayoutParams.topMargin = getStatusBarHeight()
        newLayoutParams.leftMargin = 0
        newLayoutParams.rightMargin = 0
        view.layoutParams = newLayoutParams
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = resources.getDimensionPixelSize(resourceId)
        }
        return result
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
        val locale = App.languageManager.currentLocale
        Locale.setDefault(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResourcesLocale(context, locale)
        } else updateResourcesLocaleLegacy(context, locale)
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

}
