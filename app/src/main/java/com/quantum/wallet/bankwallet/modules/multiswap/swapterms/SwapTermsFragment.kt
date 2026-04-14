package com.quantum.wallet.bankwallet.modules.multiswap.swapterms

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.setNavigationResultX
import com.quantum.wallet.bankwallet.modules.evmfee.ButtonsGroupWithShade
import com.quantum.wallet.bankwallet.modules.usersubscription.ui.highlightText
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.ui.compose.components.ButtonPrimaryYellow
import com.quantum.wallet.bankwallet.ui.compose.components.HsDivider
import com.quantum.wallet.bankwallet.ui.compose.components.VSpacer
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellLeftSelectors
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellPrimary
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs
import com.quantum.wallet.bankwallet.uiv3.components.info.TextBlock
import kotlinx.parcelize.Parcelize

class SwapTermsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        SwapTermsScreen(navController)
    }

    @Parcelize
    data class Result(val accepted: Boolean) : Parcelable
}

@Composable
fun SwapTermsScreen(navController: NavController) {
    val viewModel = viewModel<SwapTermsViewModel>(factory = SwapTermsModule.Factory())
    val uiState = viewModel.uiState
    val terms = uiState.terms

    HSScaffold(
        title = stringResource(R.string.SwapTerms_Title),
        onBack = navController::popBackStack,
        bottomBar = {
            ButtonsGroupWithShade {
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_IAgree),
                    enabled = uiState.buttonEnabled,
                    onClick = {
                        viewModel.onConfirm()

                        navController.setNavigationResultX(SwapTermsFragment.Result(true))
                        navController.popBackStack()
                    }
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            TextBlock(
                text = stringResource(R.string.SwapTerms_TopText)
            )
            VSpacer(12.dp)
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(ComposeAppTheme.colors.lawrence)
            ) {
                CellPrimary(
                    onClick = {
                        viewModel.toggleTerm(0)
                    },
                    left = {
                        CellLeftSelectors(uiState.checkboxStates[0])
                    },
                    middle = {
                        Column {
                            val highlightText = highlightText(
                                text = stringResource(terms[0].description),
                                textColor = ComposeAppTheme.colors.grey,
                                highlightPart = "(AML)",
                                highlightColor = ComposeAppTheme.colors.jacob
                            )
                            Text(
                                text = stringResource(terms[0].title),
                                style = ComposeAppTheme.typography.subhead,
                                color = ComposeAppTheme.colors.leah,
                            )
                            Text(
                                text = highlightText,
                                style = ComposeAppTheme.typography.captionSB,
                                color = ComposeAppTheme.colors.grey,
                            )
                        }
                    },
                )
                HsDivider()
                CellPrimary(
                    onClick = {
                        viewModel.toggleTerm(1)
                    },
                    left = {
                        CellLeftSelectors(uiState.checkboxStates[1])
                    },
                    middle = {
                        CellMiddleInfo(
                            subtitle = stringResource(terms[1].title).hs(
                                color = ComposeAppTheme.colors.leah
                            ),
                            description = stringResource(terms[1].description).hs
                        )
                    },
                )
            }
            TextBlock(
                text = stringResource(R.string.SwapTerms_BottomText)
            )
            VSpacer(24.dp)
        }
    }
}
