package io.horizontalsystems.bankwallet.modules.createaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.HeaderText
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.coroutines.delay

class CreateAccountFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                val popUpToInclusiveId =
                    arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.createAccountFragment) ?: R.id.createAccountFragment
                val inclusive =
                    arguments?.getBoolean(ManageAccountsModule.popOffInclusiveKey) ?: true
                CreateAccountNavHost(findNavController(), popUpToInclusiveId, inclusive)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun CreateAccountNavHost(
    fragmentNavController: NavController,
    popUpToInclusiveId: Int,
    inclusive: Boolean
) {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
        navController = navController,
        startDestination = "create_account_intro",
    ) {
        composable("create_account_intro") {
            CreateAccountIntroScreen(
                openCreateAdvancedScreen = { navController.navigate("create_account_advanced") },
                onBackClick = { fragmentNavController.popBackStack() },
                onFinish = { fragmentNavController.popBackStack(popUpToInclusiveId, inclusive) },
            )
        }
        composablePage("create_account_advanced") {
            CreateAccountAdvancedScreen(
                onBackClick = { navController.popBackStack() },
                onFinish = { fragmentNavController.popBackStack(popUpToInclusiveId, inclusive) }
            )
        }
    }
}

@Composable
private fun CreateAccountIntroScreen(
    openCreateAdvancedScreen: () -> Unit,
    onBackClick: () -> Unit,
    onFinish: () -> Unit
) {
    val viewModel = viewModel<CreateAccountViewModel>(factory = CreateAccountModule.Factory())
    val view = LocalView.current

    LaunchedEffect(viewModel.successMessage) {
        viewModel.successMessage?.let {
            HudHelper.showSuccessMessage(
                contenView = view,
                resId = it,
                icon = R.drawable.icon_add_to_wallet_24,
                iconTint = R.color.white
            )
            delay(300)

            onFinish.invoke()
            viewModel.onSuccessMessageShown()
        }
    }

    ComposeAppTheme {
        Surface(color = ComposeAppTheme.colors.tyler) {
            Column(Modifier.fillMaxSize()) {
                AppBar(
                    title = TranslatableString.ResString(R.string.ManageAccounts_CreateNewWallet),
                    menuItems = listOf(
                        MenuItem(
                            title = TranslatableString.ResString(R.string.Button_Create),
                            onClick = viewModel::createAccount
                        )
                    ),
                    navigationIcon = {
                        HsBackButton(onClick = onBackClick)
                    },
                    backgroundColor = Color.Transparent
                )
                Spacer(Modifier.height(12.dp))

                HeaderText(stringResource(id = R.string.ManageAccount_Name))
                FormsInput(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    initial = viewModel.accountName,
                    pasteEnabled = false,
                    hint = viewModel.defaultAccountName,
                    onValueChange = viewModel::onChangeAccountName
                )

                Spacer(Modifier.height(32.dp))

                CellSingleLineLawrenceSection {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                openCreateAdvancedScreen.invoke()
                            }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        body_leah(text = stringResource(R.string.Button_Advanced))
                        Spacer(modifier = Modifier.weight(1f))
                        Image(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(id = R.drawable.ic_arrow_right),
                            contentDescription = null,
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
