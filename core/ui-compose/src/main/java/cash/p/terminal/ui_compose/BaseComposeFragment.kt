package cash.p.terminal.ui_compose

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.LayoutRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import cash.p.terminal.ui_compose.components.ConnectionStatusView
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

val LocalConnectionPanelState = compositionLocalOf {
    mutableStateOf(true)
}

abstract class BaseComposeFragment(
    @LayoutRes layoutResId: Int = 0,
    private val screenshotEnabled: Boolean = true
) : Fragment(layoutResId) {

    open val showConnectionPanel: Boolean = true

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
                    val navController = findNavController()

                    // Capture this fragment's destination ID on first composition,
                    // then use it to skip rendering if the destination has been
                    // popped — prevents navGraphViewModels crashes during
                    // destruction recomposition.
                    val destId = remember { navController.currentDestination?.id }
                    val isOnBackStack = remember(navController.currentBackStackEntry) {
                        destId == null || try {
                            navController.getBackStackEntry(destId)
                            true
                        } catch (_: IllegalArgumentException) {
                            false
                        }
                    }

                    if (isOnBackStack) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            GetContent(navController)
                            if (showConnectionPanel && LocalConnectionPanelState.current.value) {
                                ConnectionStatusView(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .navigationBarsPadding()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    protected inline fun <reified T : Parcelable> withInput(
        navController: NavController,
        content: @Composable (T) -> Unit
    ) {
        val input = remember {
            try {
                navController.requireInput<T>()
            } catch (_: Exception) {
                null
            }
        }
        if (input == null) {
            LaunchedEffect(Unit) {
                navController.navigateUp()
            }
            return
        }
        content(input)
    }

    @Composable
    abstract fun GetContent(navController: NavController)

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

    private fun allowScreenshot() {
        if (ScreenSecurityState.isAppLocked) return
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun disallowScreenshot() {
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

}