package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.NavController

abstract class BaseFragment : HSScreen() {
    val viewLifecycleOwner: LifecycleOwner = TODO()

    open fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        TODO()
    }

    fun requireContext(): Context {
        TODO()
    }

    fun findNavController() : NavController {
        TODO()
    }

    public inline fun <reified VM : ViewModel> navGraphViewModels(
        @IdRes navGraphId: Int,
        noinline extrasProducer: (() -> CreationExtras)? = null,
        noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
    ): Lazy<VM> {
        TODO()
    }

    public inline fun <reified VM : ViewModel> viewModels(
        noinline ownerProducer: () -> ViewModelStoreOwner = { TODO() },
        noinline extrasProducer: (() -> CreationExtras)? = null,
        noinline factoryProducer: (() -> Factory)? = null
    ): Lazy<VM> {
        TODO()
    }

}

abstract class BaseFragmentX(
    @LayoutRes layoutResId: Int = 0,
    private val screenshotEnabled: Boolean = true
) : Fragment(layoutResId) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        TODO()

    }

    override fun onResume() {
        super.onResume()
        if (screenshotEnabled) {
            allowScreenshot()
        } else {
            disallowScreenshot()
        }
    }

    override fun onPause() {
        disallowScreenshot()
        super.onPause()
    }

    protected fun hideKeyboard() {
        activity?.getSystemService(InputMethodManager::class.java)?.hideSoftInputFromWindow(activity?.currentFocus?.windowToken, 0)
    }

    protected fun setMenuItemEnabled(menuItem: MenuItem, enabled: Boolean) {
        menuItem.isEnabled = enabled
        context?.let { ctx ->
            val color = ctx.getColor(if (enabled) R.color.jacob else R.color.grey)
            val spannable = SpannableString(menuItem.title)
            spannable.setSpan(ForegroundColorSpan(color), 0, spannable.length, 0)
            menuItem.title = spannable
        }
    }

    private fun allowScreenshot() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun disallowScreenshot() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

}
