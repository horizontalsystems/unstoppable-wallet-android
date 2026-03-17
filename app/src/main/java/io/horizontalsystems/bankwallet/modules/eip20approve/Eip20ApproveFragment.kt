package io.horizontalsystems.bankwallet.modules.eip20approve

import android.os.Parcelable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.evmfee.ButtonsGroupWithShade
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.ResultEffect
import io.horizontalsystems.bankwallet.serializers.BigDecimalSerializer
import io.horizontalsystems.bankwallet.serializers.TokenSerializer
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.HsCheckbox
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.cell.SectionUniversalLawrence
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Eip20ApproveScreen(
    @Serializable(with = TokenSerializer::class)
    val token: Token,
    @Serializable(with = BigDecimalSerializer::class)
    val requiredAllowance: BigDecimal,
    val spenderAddress: String
) : HSScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        Eip20ApproveScreen(backStack, token, requiredAllowance, spenderAddress)
    }
}

class Eip20ApproveFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
    }

    @Parcelize
    data class Input(
        val token: Token,
        val requiredAllowance: BigDecimal,
        val spenderAddress: String
    ) : Parcelable
}

@Composable
fun Eip20ApproveScreen(
    backStack: NavBackStack<HSScreen>,
    token: Token,
    requiredAllowance: BigDecimal,
    spenderAddress: String
) {
    val resultBus = LocalResultEventBus.current
    val viewModel = viewModel<Eip20ApproveViewModel>(
        factory = Eip20ApproveViewModel.Factory(
            token,
            requiredAllowance,
            spenderAddress,
        )
    )

    val uiState = viewModel.uiState

    HSScaffold(
        title = stringResource(R.string.Swap_Approve_PageTitle),
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Button_Close),
                icon = R.drawable.ic_close,
                onClick = backStack::removeLastOrNull
            )
        ),
        bottomBar = {
            ButtonsGroupWithShade {
                ResultEffect<Eip20ApproveConfirmScreen.Result> {
                    resultBus.sendResult(result = it)
                    backStack.removeLastOrNull()
                }
                ButtonPrimaryYellow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    title = stringResource(R.string.Button_Next),
                    onClick = {
                        viewModel.freeze()
                        backStack.add(Eip20ApproveConfirmScreen)
                    },
                )
            }
        },
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            VSpacer(height = 12.dp)

            SectionUniversalLawrence {
                val setOnlyRequired = { viewModel.setAllowanceMode(AllowanceMode.OnlyRequired) }
                CellUniversal(
                    onClick = setOnlyRequired
                ) {
                    HsCheckbox(
                        checked = uiState.allowanceMode == AllowanceMode.OnlyRequired,
                        onCheckedChange = { setOnlyRequired.invoke() }
                    )
                    HSpacer(width = 16.dp)
                    val coinValue = CoinValue(
                        uiState.token,
                        uiState.requiredAllowance
                    ).getFormattedFull()
                    subhead2_leah(text = coinValue)
                }

                val setUnlimited = { viewModel.setAllowanceMode(AllowanceMode.Unlimited) }
                CellUniversal(
                    borderTop = true,
                    onClick = setUnlimited
                ) {
                    HsCheckbox(
                        checked = uiState.allowanceMode == AllowanceMode.Unlimited,
                        onCheckedChange = { setUnlimited.invoke() }
                    )
                    HSpacer(width = 16.dp)
                    subhead2_leah(text = stringResource(id = R.string.Swap_Approve_Unlimited))
                }
            }
            InfoText(text = stringResource(R.string.Swap_Approve_Info))
            VSpacer(height = 32.dp)
        }
    }
}
