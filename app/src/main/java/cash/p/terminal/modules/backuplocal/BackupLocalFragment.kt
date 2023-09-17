package cash.p.terminal.modules.backuplocal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import cash.p.terminal.core.BaseFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.modules.backuplocal.password.LocalBackupPasswordScreen
import cash.p.terminal.modules.backuplocal.terms.LocalBackupTermsScreen
import io.horizontalsystems.core.findNavController

class BackupLocalFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                BackupLocalNavHost(findNavController())
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun BackupLocalNavHost(fragmentNavController: NavController) {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navController,
        startDestination = "terms_page",
    ) {
        composable("terms_page") { LocalBackupTermsScreen(fragmentNavController, navController) }
        composablePage("password_page") { LocalBackupPasswordScreen(fragmentNavController, navController) }
    }
}
