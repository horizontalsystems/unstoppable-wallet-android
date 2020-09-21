package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.views.AlertDialogFragment

abstract class BaseFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = TransitionInflater.from(context).inflateTransition(R.transition.slide_right)
    }

    protected fun hideKeyboard() {
        activity?.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
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
