package io.horizontalsystems.bankwallet

import android.content.Context
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenActivity
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenModule
import io.horizontalsystems.core.CoreActivity
import io.horizontalsystems.views.AlertDialogFragment
import java.util.logging.Logger

abstract class BaseActivity : CoreActivity() {

    private val logger = Logger.getLogger("BaseActivity")

    override fun onResume() {
        super.onResume()
        if (this !is LockScreenActivity && App.lockManager.isLocked) {
            LockScreenModule.startForUnlock(this, 1)
        }
    }

    fun showCustomKeyboardAlert() {
        AlertDialogFragment.newInstance(
                getString(R.string.Alert_TitleWarning),
                getString(R.string.Alert_CustomKeyboardIsUsed),
                R.string.Alert_Ok,
                false,
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

    protected fun setTransparentStatusBar() {
        val oldFlags = window.decorView.systemUiVisibility
        window.decorView.systemUiVisibility = oldFlags or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    protected fun hideSoftKeyboard() {
        getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

}
