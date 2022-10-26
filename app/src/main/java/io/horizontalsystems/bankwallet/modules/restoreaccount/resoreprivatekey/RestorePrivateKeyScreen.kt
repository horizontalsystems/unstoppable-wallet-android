package io.horizontalsystems.bankwallet.modules.restoreaccount.resoreprivatekey

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.restoreaccount.restore.RestoreByMenu
import io.horizontalsystems.bankwallet.modules.restoreaccount.restore.RestoreViewModel
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreblockchains.RestoreBlockchainsFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputMultiline
import io.horizontalsystems.bankwallet.ui.compose.components.HsIconButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem

@Composable
fun ResorePrivateKey(
    navController: NavController,
    popUpToInclusiveId: Int,
    restoreViewModel: RestoreViewModel,
) {
    val viewModel =
        viewModel<RestorePrivateKeyViewModel>(factory = RestorePrivateKeyModule.Factory())

    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = TranslatableString.ResString(R.string.Restore_Enter_Key_Title),
                navigationIcon = {
                    HsIconButton(onClick = navController::popBackStack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "back",
                            tint = ComposeAppTheme.colors.jacob
                        )
                    }
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Next),
                        onClick = {
                            viewModel.resolveAccountType()?.let { accountType ->
                                navController.slideFromRight(
                                    R.id.restoreSelectCoinsFragment,
                                    bundleOf(
                                        RestoreBlockchainsFragment.ACCOUNT_NAME_KEY to viewModel.defaultName,
                                        RestoreBlockchainsFragment.ACCOUNT_TYPE_KEY to accountType,
                                        ManageAccountsModule.popOffOnSuccessKey to popUpToInclusiveId,
                                    )
                                )
                            }
                        }
                    )
                )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))

            RestoreByMenu(restoreViewModel)

            Spacer(Modifier.height(24.dp))

            FormsInputMultiline(
                modifier = Modifier.padding(horizontal = 16.dp),
                hint = stringResource(id = R.string.Restore_PrivateKeyHint),
                state = viewModel.inputState,
                qrScannerEnabled = true,
            ) {
                viewModel.onEnterPrivateKey(it)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
