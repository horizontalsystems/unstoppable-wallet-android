package io.horizontalsystems.walletkit.modules.privatesend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.multiswap.SwapInfoSheet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HSpacer
import io.horizontalsystems.walletkit.ui.compose.components.HsSwitch
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.body_leah
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.reflect.KClass

/**
 * State of the private send toggle on a send screen. Deliberately tiny: it does no quoting
 * and holds no orders — all network work happens on the confirmation screen. Support is a
 * synchronous read of the already-background-synced confidential token cache.
 */
class PrivateSendViewModel(
    private val token: Token,
    private val manager: PrivateSendManager = App.privateSendManager,
) : ViewModel() {

    var isSupported by mutableStateOf(manager.isSupported(token))
        private set

    var isEnabled by mutableStateOf(false)
        private set

    // The entered amount, mirrored from the screen's amount input: with the toggle on it is
    // the exact output the recipient must receive, which the confirmation page commits on.
    var amount by mutableStateOf<BigDecimal?>(null)
        private set

    // Bitcoin-family only: the send screen's settings, set right before opening the
    // confirmation, which resolves this same ViewModel from the SendPage store to build the
    // deposit with them. Plain memory — lost on process death, which degrades to defaults.
    var btcParams: PrivateSendBtcParams? = null
        private set

    fun setBtcDepositParams(params: PrivateSendBtcParams) {
        btcParams = params
    }

    init {
        viewModelScope.launch {
            // TTL-guarded and cheap; a first token-list sync landing while this screen is
            // open reveals the toggle without a reload.
            manager.sync()
        }

        viewModelScope.launch {
            manager.availabilityFlow.collect {
                val supported = manager.isSupported(token)
                if (supported != isSupported) {
                    isSupported = supported

                    // Never leave the toggle on for a token that can no longer route.
                    if (!supported) {
                        isEnabled = false
                    }
                }
            }
        }
    }

    fun onToggle(enabled: Boolean) {
        isEnabled = enabled
    }

    fun onEnterAmount(amount: BigDecimal?) {
        this.amount = amount
    }

    /**
     * The single Next-button branch every send screen uses: with the toggle off it does
     * nothing and the screen proceeds to its own confirmation; with it on, it opens the
     * private send confirmation and the screen must go no further. Centralised so the
     * eleven call sites cannot drift in how they build the page input.
     */
    fun openConfirmationIfEnabled(
        navigation: HSNavigation,
        wallet: Wallet,
        recipient: String,
        sendEntryPointDestId: KClass<out HSPage>?,
    ): Boolean {
        if (!isEnabled) return false

        // No amount can only mean the screen let Next through before its own amount
        // validation passed; falling through to a REGULAR send here would be worse.
        val amount = amount ?: return true

        navigation.slideFromRight(
            PrivateSendConfirmationPage(
                PrivateSendConfirmationPage.Input(
                    wallet = wallet,
                    recipient = recipient,
                    amount = amount,
                    sendEntryPointDestId = sendEntryPointDestId,
                )
            )
        )

        return true
    }

    class Factory(private val token: Token) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PrivateSendViewModel(token) as T
        }
    }
}

// Default-keyed on purpose: the confirmation page looks this instance up from the SendPage
// store by class, and a SendPage hosts exactly one token, so no custom key is needed.
@Composable
fun privateSendViewModel(token: Token): PrivateSendViewModel =
    viewModel(factory = PrivateSendViewModel.Factory(token))

/**
 * The toggle row, rendered on a send screen between the balance row and the memo/button
 * area. Not rendered at all for an unsupported token, not merely disabled. Matches the
 * Figma "Private Send" row: incognito icon, label, info button (opens the explanation
 * sheet — the row itself carries no subtitle), switch.
 */
@Composable
fun PrivateSendToggleSection(viewModel: PrivateSendViewModel, navigation: HSNavigation) {
    if (!viewModel.isSupported) return

    val infoTitle = stringResource(R.string.PrivateSend_Toggle_Title)
    val infoText = stringResource(R.string.PrivateSend_Toggle_Subtitle)

    VSpacer(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { viewModel.onToggle(!viewModel.isEnabled) }
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HSpacer(16.dp)
        Icon(
            painter = painterResource(R.drawable.ic_incognito_24),
            contentDescription = null,
            tint = ComposeAppTheme.colors.leah,
            modifier = Modifier.size(24.dp),
        )
        HSpacer(16.dp)
        body_leah(text = infoTitle)
        HSpacer(8.dp)
        Icon(
            painter = painterResource(R.drawable.ic_info_filled_20),
            contentDescription = null,
            tint = ComposeAppTheme.colors.grey,
            modifier = Modifier
                .size(20.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        navigation.slideFromBottom(SwapInfoSheet(SwapInfoSheet.Input(infoTitle, infoText)))
                    }
                ),
        )
        Spacer(modifier = Modifier.weight(1f))
        HsSwitch(
            checked = viewModel.isEnabled,
            onCheckedChange = { viewModel.onToggle(it) }
        )
        HSpacer(16.dp)
    }
}
