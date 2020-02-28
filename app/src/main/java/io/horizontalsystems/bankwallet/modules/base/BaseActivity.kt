package io.horizontalsystems.bankwallet.modules.base

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenActivity
import io.horizontalsystems.bankwallet.modules.lockscreen.LockScreenModule
import io.horizontalsystems.bankwallet.modules.tor.TorConnectionActivity
import io.horizontalsystems.core.CoreActivity
import io.horizontalsystems.views.AlertDialogFragment
import java.util.logging.Logger

abstract class BaseActivity : CoreActivity() {

    private val logger = Logger.getLogger("BaseActivity")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val presenter = ViewModelProvider(this, BaseModule.Factory()).get(BasePresenter::class.java)
        presenter.viewDidLoad()

        observeView(presenter.view as BaseView)
    }

    private fun observeView(baseView: BaseView) {
        baseView.torConnectionStatus.observe(this, Observer {
            //check if its foreground, call only once for whole
            val isActivityInForeground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            if(isActivityInForeground) {
                val intent = Intent(this, TorConnectionActivity::class.java)
                startActivity(intent)
            }
        })
    }

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
                        val imeManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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
