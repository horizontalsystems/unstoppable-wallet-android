package io.horizontalsystems.bankwallet.modules.backuplocal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.modules.backuplocal.password.LocalBackupPasswordScreen
import io.horizontalsystems.bankwallet.modules.backuplocal.terms.LocalBackupTermsScreen
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
                BackupLocalNavHost(findNavController(), requireArguments().getString(ACCOUNT_ID_KEY))
            }
        }
    }

    companion object {
        private const val ACCOUNT_ID_KEY = "coin_uid_key"
        fun prepareParams(accountId: String): Bundle {
            return bundleOf(ACCOUNT_ID_KEY to accountId)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun BackupLocalNavHost(fragmentNavController: NavController, accountId: String?) {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navController,
        startDestination = "terms_page",
    ) {
        composable("terms_page") {
            LocalBackupTermsScreen(fragmentNavController, navController)
        }
        composablePage("password_page") {
            LocalBackupPasswordScreen(
                fragmentNavController,
                navController,
                accountId
            )
        }
    }
}
