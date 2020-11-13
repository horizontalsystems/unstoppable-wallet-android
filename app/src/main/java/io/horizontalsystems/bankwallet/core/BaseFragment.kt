package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.graphics.PorterDuff
import android.os.Handler
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.core.navigation.NavDestinationChangeListener
import io.horizontalsystems.views.AlertDialogFragment

abstract class BaseFragment : Fragment() {

    protected fun hideKeyboard() {
        activity?.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }

    protected fun setNavigationToolbar(toolbar: Toolbar, navController: NavController){
        val appBarConfiguration = AppBarConfiguration(navController.graph)

        val navDestinationChangeListener = NavDestinationChangeListener(toolbar, appBarConfiguration, true)
        navController.addOnDestinationChangedListener(navDestinationChangeListener)
        toolbar.setNavigationOnClickListener { NavigationUI.navigateUp(navController, appBarConfiguration) }
    }

    protected fun setMenuItemEnabled(menuItem: MenuItem, enabled: Boolean) {
        menuItem.isEnabled = enabled
        val color = context?.getColor(if (enabled) R.color.yellow_d else R.color.grey) ?: return
        menuItem.icon.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    protected fun navOptions(): NavOptions {
        return NavOptions.Builder()
                .setEnterAnim(R.anim.from_right)
                .setExitAnim(R.anim.to_left)
                .setPopEnterAnim(R.anim.from_left)
                .setPopExitAnim(R.anim.to_right)
                .build()
    }

    protected fun navOptionsFromBottom(): NavOptions {
        return NavOptions.Builder()
                .setEnterAnim(R.anim.slide_in_bottom)
                .setExitAnim(R.anim.to_top)
                .setPopEnterAnim(R.anim.from_top)
                .setPopExitAnim(R.anim.slide_out_bottom)
                .build()
    }

    protected fun showCustomKeyboardAlert() {
        AlertDialogFragment.newInstance(
                titleString = getString(R.string.Alert_TitleWarning),
                descriptionString = getString(R.string.Alert_CustomKeyboardIsUsed),
                buttonText = R.string.Alert_Ok,
                cancelable = false,
                listener = object : AlertDialogFragment.Listener {
                    override fun onButtonClick() {
                        val imeManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imeManager.showInputMethodPicker()
                        hideKeyboard()
                        Handler().postDelayed({
                            try {
                                requireActivity().onBackPressed()
                            } catch (e: NullPointerException) {
                                //do nothing
                            }
                        }, (1 * 750).toLong())
                    }

                    override fun onCancel() {}
                }).show(parentFragmentManager, "custom_keyboard_alert")
    }
}
