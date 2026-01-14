package io.horizontalsystems.bankwallet.modules.multiswap.swapterms

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
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.highlightText
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HsDivider
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftSelectors
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.bankwallet.uiv3.components.info.TextBlock

class SwapTermsFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        SwapTermsScreen {
            navController.popBackStack()
        }
    }
}

@Composable
fun SwapTermsScreen(
    viewModel: SwapTermsViewModel = viewModel(factory = SwapTermsModule.Factory()),
    onCloseClick: () -> Unit
) {
    val titleColor = ComposeAppTheme.colors.leah
    val uiState = viewModel.uiState
    val terms = uiState.terms

    HSScaffold(
        title = stringResource(R.string.SwapTerms_Title),
        onBack = onCloseClick,
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
                        onCloseClick()
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
                                color = titleColor
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
