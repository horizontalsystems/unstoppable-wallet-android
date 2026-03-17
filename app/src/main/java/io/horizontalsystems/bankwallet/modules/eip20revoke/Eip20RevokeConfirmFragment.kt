package io.horizontalsystems.bankwallet.modules.eip20revoke

import android.os.Parcelable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.confirm.ConfirmTransactionScreen
import io.horizontalsystems.bankwallet.modules.confirm.ErrorBottomSheetScreen
import io.horizontalsystems.bankwallet.modules.eip20approve.ConfirmTokenSection
import io.horizontalsystems.bankwallet.modules.eip20approve.SpenderCell
import io.horizontalsystems.bankwallet.modules.evmfee.Cautions
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataFieldFeeTemplate
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.serializers.BigDecimalSerializer
import io.horizontalsystems.bankwallet.serializers.TokenSerializer
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class Eip20RevokeConfirmScreen(
    @Serializable(with = TokenSerializer::class)
    val token: Token,
    val spenderAddress: String,
    @Serializable(with = BigDecimalSerializer::class)
    val allowance: BigDecimal,
) : HSScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        Eip20RevokeScreen(
            backStack,
            token,
            spenderAddress,
            allowance
        )
    }

    data class Result(val revoked: Boolean)
}

class Eip20RevokeConfirmFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
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
fun Eip20RevokeScreen(
    backStack: NavBackStack<HSScreen>,
    token: Token,
    spenderAddress: String,
    allowance: BigDecimal
) {
    val resultBus = LocalResultEventBus.current
    val viewModel = viewModel<Eip20RevokeConfirmViewModel>(
        factory = Eip20RevokeConfirmViewModel.Factory(
            token,
            spenderAddress,
            allowance
        )
    )

    val uiState = viewModel.uiState
    val view = LocalView.current

    ConfirmTransactionScreen(
        title = stringResource(R.string.Swap_ConfirmRevoke_Title),
        initialLoading = uiState.initialLoading,
        onClickBack = backStack::removeLastOrNull,
        onClickFeeSettings = {
            backStack.add(Eip20RevokeTransactionSettingsScreen)
        },
        buttonsSlot = {
            val coroutineScope = rememberCoroutineScope()
            var buttonEnabled by remember { mutableStateOf(true) }
            var buttonTitle by remember { mutableIntStateOf(R.string.Swap_Revoke) }

            ButtonPrimaryYellow(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(buttonTitle),
                onClick = {
                    coroutineScope.launch {
                        buttonEnabled = false
                        buttonTitle = R.string.Swap_Revoking

                        try {
                            viewModel.revoke()

                            HudHelper.showSuccessMessage(view, R.string.Hud_Text_Done)
                            delay(1200)
                            resultBus.sendResult(result = Eip20RevokeConfirmScreen.Result(true))
                            backStack.removeLastOrNull()
                        } catch (t: Throwable) {
                            backStack.add(ErrorBottomSheetScreen(t.message ?: t.javaClass.simpleName))
                        }

                        buttonTitle = R.string.Swap_Revoke
                        buttonEnabled = true
                    }
                },
                enabled = uiState.revokeEnabled && buttonEnabled
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
                backStack = backStack,
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
