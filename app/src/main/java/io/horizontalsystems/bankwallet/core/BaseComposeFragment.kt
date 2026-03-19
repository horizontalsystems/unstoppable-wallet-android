package io.horizontalsystems.bankwallet.core

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.NavController
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import kotlin.reflect.KClass

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

    @Composable
    protected inline fun <reified T : Parcelable> withInput(
        navController: NavController,
        content: @Composable (T) -> Unit
    ) {
        val input = try {
            navController.requireInput<T>()
        } catch (e: NullPointerException) {
            navController.popBackStack()
            return
        }
        content(input)
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

    fun findNavController(): NavController {
        TODO()
    }

    fun requireView() : View {
        TODO()
    }

    @MainThread
    public inline fun <reified VM : ViewModel> viewModels(
        noinline ownerProducer: () -> ViewModelStoreOwner = { TODO() },
        noinline extrasProducer: (() -> CreationExtras)? = null,
        noinline factoryProducer: (() -> Factory)? = null
    ): Lazy<VM> {
        val owner by lazy(LazyThreadSafetyMode.NONE) { ownerProducer() }
        return createViewModelLazy(
            VM::class,
            { owner.viewModelStore },
            {
                extrasProducer?.invoke()
                    ?: (owner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras
                    ?: CreationExtras.Empty
            },
            factoryProducer ?: {
                (owner as? HasDefaultViewModelProviderFactory)?.defaultViewModelProviderFactory
                    ?: defaultViewModelProviderFactory
            })
    }

    @MainThread
    public fun <VM : ViewModel> createViewModelLazy(
        viewModelClass: KClass<VM>,
        storeProducer: () -> ViewModelStore,
        extrasProducer: () -> CreationExtras = { defaultViewModelCreationExtras },
        factoryProducer: (() -> Factory)? = null

    ): Lazy<VM> {
        val factoryPromise = factoryProducer ?: {
            defaultViewModelProviderFactory
        }
        return ViewModelLazy(viewModelClass, storeProducer, factoryPromise, extrasProducer)
    }

    @MainThread
    public inline fun <reified VM : ViewModel> navGraphViewModels(
        @IdRes navGraphId: Int,
        noinline extrasProducer: (() -> CreationExtras)? = null,
        noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
    ): Lazy<VM> {
        TODO()
//        val backStackEntry by lazy { findNavController().getBackStackEntry(navGraphId) }
//        val storeProducer: () -> ViewModelStore = { backStackEntry.viewModelStore }
//        return createViewModelLazy(
//            VM::class,
//            storeProducer,
//            { extrasProducer?.invoke() ?: backStackEntry.defaultViewModelCreationExtras },
//            factoryProducer ?: { backStackEntry.defaultViewModelProviderFactory }
//        )
    }


    fun requireArguments() : Bundle {
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
    protected inline fun <reified T : Parcelable> withInput(
        navController: NavController,
        content: @Composable (T) -> Unit
    ) {
        val input = try {
            navController.requireInput<T>()
        } catch (e: NullPointerException) {
            navController.popBackStack()
            return
        }
        content(input)
    }

    @Composable
    abstract fun GetContent(navController: NavController)

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