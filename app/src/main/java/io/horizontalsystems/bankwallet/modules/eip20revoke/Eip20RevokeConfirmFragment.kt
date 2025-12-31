package io.horizontalsystems.bankwallet.modules.eip20revoke

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.eip20approve.ConfirmTokenSection
import io.horizontalsystems.bankwallet.modules.eip20approve.SpenderCell
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFeeTemplate
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.SnackbarDuration
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

class Eip20RevokeConfirmFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            Eip20RevokeScreen(navController, input)
        }
    }

    @Parcelize
    data class Input(
        val token: Token,
        val spenderAddress: String,
        val allowance: BigDecimal,
    ) : Parcelable

    @Parcelize
    data class Result(val revoked: Boolean) : Parcelable
}

@Composable
fun Eip20RevokeScreen(navController: NavController, input: Eip20RevokeConfirmFragment.Input) {
    val currentBackStackEntry = remember(navController.currentBackStackEntry) {
        navController.getBackStackEntry(R.id.eip20RevokeConfirmFragment)
    }
    val viewModel = viewModel<Eip20RevokeConfirmViewModel>(
        viewModelStoreOwner = currentBackStackEntry,
        factory = Eip20RevokeConfirmViewModel.Factory(
            input.token,
            input.spenderAddress,
            input.allowance
        )
    )

    val uiState = viewModel.uiState
    val view = LocalView.current

    ConfirmTransactionScreen(
        title = stringResource(R.string.Swap_ConfirmRevoke_Title),
        onClickBack = navController::popBackStack,
        onClickSettings = {
            navController.slideFromRight(R.id.eip20RevokeTransactionSettingsFragment)
        },
        onClickClose = navController::popBackStack,
        buttonsSlot = {
            val coroutineScope = rememberCoroutineScope()
            var buttonEnabled by remember { mutableStateOf(true) }

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Swap_Revoke),
                onClick = {
                    coroutineScope.launch {
                        buttonEnabled = false
                        HudHelper.showInProcessMessage(
                            view,
                            R.string.Swap_Revoking,
                            SnackbarDuration.INDEFINITE
                        )

                        val result = try {
                            viewModel.revoke()

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                            Eip20RevokeConfirmFragment.Result(true)
                        } catch (t: Throwable) {
                            HudHelper.showErrorMessage(view, t.javaClass.simpleName)
                            Eip20RevokeConfirmFragment.Result(false)
                        }

                        buttonEnabled = true
                        navController.setNavigationResultX(result)
                        navController.popBackStack()
                    }
                },
                enabled = uiState.revokeEnabled && buttonEnabled
            )
            VSpacer(16.dp)
            ButtonPrimaryDefault(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.Button_Cancel),
                onClick = {
                    navController.popBackStack()
                }
            )
        }
    ) {
        ConfirmTokenSection(
            token = uiState.token,
            amount = uiState.allowance,
            fiatAmount = uiState.fiatAmount,
            currency = uiState.currency,
        )
        VSpacer(16.dp)
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ComposeAppTheme.colors.lawrence)
                .padding(vertical = 8.dp)
        ) {
            SpenderCell(
                address = uiState.spenderAddress,
                contact = uiState.contact?.name,
                onCopyClick = {
                    TextHelper.copyText(uiState.spenderAddress)
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
                }
            )

            DataFieldFeeTemplate(
                navController = navController,
                primary = uiState.networkFee?.primary?.getFormattedPlain() ?: "---",
                secondary = uiState.networkFee?.secondary?.getFormattedPlain(),
                title = stringResource(id = R.string.FeeSettings_NetworkFee),
                infoText = stringResource(id = R.string.FeeSettings_NetworkFee_Info)
            )
        }

        if (uiState.cautions.isNotEmpty()) {
            Cautions(cautions = uiState.cautions)
        }
    }
}
