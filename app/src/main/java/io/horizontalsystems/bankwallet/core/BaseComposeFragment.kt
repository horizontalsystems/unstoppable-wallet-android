package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

abstract class BaseComposeFragment(
    screenshotEnabled: Boolean = true
) : HSScreen(screenshotEnabled = screenshotEnabled), LifecycleOwner {
    override val lifecycle: Lifecycle
        get() = TODO("Not yet implemented")

    open fun onCreate(savedInstanceState: Bundle?) {

    }

    open fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }

    fun requireActivity() : FragmentActivity {
        TODO()
    }

    val defaultViewModelProviderFactory: Factory
        get() = TODO()
    val defaultViewModelCreationExtras: CreationExtras
        get() = TODO()

    val childFragmentManager: FragmentManager
        get() = TODO()
    val activity: FragmentActivity?
        get() = TODO()

    fun getString(i: Int) : String {
        TODO()
    }

    fun requireContext(): Context {
        TODO()
    }

    fun findNavController(): NavBackStack<HSScreen> {
        TODO()
    }

    fun requireView() : View {
        TODO()
    }
}

abstract class BaseComposeFragmentX(
    @LayoutRes layoutResId: Int = 0,
    private val screenshotEnabled: Boolean = true
) : Fragment(layoutResId) {

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                ComposeAppTheme {
//                    GetContent(findNavController())
                }
            }
        }
    }

    @Composable
    abstract fun GetContent(navController: NavBackStack<HSScreen>)

    override fun onResume() {
        super.onResume()
        Log.i("AAA", "Fragment: ${this.javaClass.simpleName}")
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

    private fun allowScreenshot() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun disallowScreenshot() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

}