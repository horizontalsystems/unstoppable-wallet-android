package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.AlertDialogKeyboardFragment

abstract class BaseFragment : Fragment() {

    protected fun hideKeyboard() {
        activity?.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }

    protected fun setMenuItemEnabled(menuItem: MenuItem, enabled: Boolean) {
        menuItem.isEnabled = enabled
        context?.let { ctx ->
            val color = ctx.getColor(if (enabled) R.color.jacob else R.color.grey)
            menuItem.icon.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP)
        }
    }

    protected fun navOptions(): NavOptions {
        return NavOptions.Builder()
                .setEnterAnim(R.anim.slide_from_right)
                .setExitAnim(R.anim.slide_to_left)
                .setPopEnterAnim(R.anim.slide_from_left)
                .setPopExitAnim(R.anim.slide_to_right)
                .build()
    }

    protected fun navOptionsFromBottom(): NavOptions {
        return NavOptions.Builder()
                .setEnterAnim(R.anim.slide_from_bottom)
                .setExitAnim(R.anim.slide_to_top)
                .setPopEnterAnim(R.anim.slide_from_top)
                .setPopExitAnim(R.anim.slide_to_bottom)
                .build()
    }

    fun showCustomKeyboardAlert() {
        AlertDialogKeyboardFragment.newInstance(
                titleString = getString(R.string.Alert_TitleWarning),
                descriptionString = getString(R.string.Alert_CustomKeyboardIsUsed),
                selectButtonText = R.string.Alert_Select,
                skipButtonText = R.string.Alert_Skip,
                listener = object : AlertDialogKeyboardFragment.Listener {
                    override fun onButtonClick() {
                        val imeManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imeManager.showInputMethodPicker()
                        hideKeyboard()
                        Handler(Looper.getMainLooper()).postDelayed({
                            try {
                                requireActivity().onBackPressed()
                            } catch (e: NullPointerException) {
                                //do nothing
                            }
                        }, (1 * 750).toLong())
                    }


                    override fun onCancel() {}
                    override fun onSkipClick() {
                        App.thirdKeyboardStorage.isThirdPartyKeyboardAllowed = true
                    }
                }).show(parentFragmentManager, "custom_keyboard_alert")
    }
}
