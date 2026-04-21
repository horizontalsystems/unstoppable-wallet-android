package io.horizontalsystems.bankwallet.modules.manageaccounts

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.BoxBordered
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.Section
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftSelectors
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import kotlinx.parcelize.Parcelize

class PassKeyTermsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        PasskeyTermsScreen(navController = navController)
    }

    @Parcelize
    data class Result(val termsAccepted: Boolean) : Parcelable
}

@Composable
fun PasskeyTermsScreen(
    navController: NavController,
    viewModel: PasskeyTermsViewModel = viewModel(factory = PasskeyTermsModule.Factory())
) {

    val uiState = viewModel.uiState

    LaunchedEffect(uiState.closeScreen) {
        if (uiState.closeScreen) {
            navController.setNavigationResultX(PassKeyTermsFragment.Result(true))
            navController.popBackStack()
        }
    }

    HSScaffold(
        title = stringResource(R.string.CreateNewWallet_PasskeyTerms),
        onBack = navController::popBackStack,
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(12.dp)

                Section {
                    viewModel.uiState.terms.forEachIndexed { index, item ->
                        PasskeyTermCell(
                            title = stringResource(item.term.title),
                            subtitle = stringResource(item.term.description),
                            checked = item.checked,
                            showBorder = index != 0,
                            onClick = {
                                viewModel.onCheck(item.term)
                            }
                        )
                    }
                }

                VSpacer(56.dp)
            }

            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.Button_UnderstandAndContinue),
                    onClick = { viewModel.onContinueClick() },
                    enabled = viewModel.uiState.buttonEnabled
                )
            }
        }
    }
}

@Composable
fun PasskeyTermCell(
    title: String,
    subtitle: String,
    checked: Boolean,
    showBorder: Boolean,
    onClick: () -> Unit
) {
    BoxBordered(top = showBorder) {
        CellPrimary(
            left = {
                CellLeftSelectors(checked)
            },
            middle = {
                CellMiddleInfo(
                    eyebrow = title.hs(ComposeAppTheme.colors.leah),
                    subtitle = subtitle.hs
                )
            },
            onClick = onClick
        )
    }
}
