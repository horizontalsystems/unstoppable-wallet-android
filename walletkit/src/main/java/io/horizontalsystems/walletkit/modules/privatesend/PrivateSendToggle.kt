package io.horizontalsystems.walletkit.modules.privatesend

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HsSwitch
import io.horizontalsystems.walletkit.ui.compose.components.RowUniversal
import io.horizontalsystems.walletkit.ui.compose.components.VSpacer
import io.horizontalsystems.walletkit.ui.compose.components.body_leah
import io.horizontalsystems.walletkit.ui.compose.components.subhead2_grey
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

@Composable
fun privateSendViewModel(token: Token): PrivateSendViewModel =
    viewModel(key = "private_send_${token.tokenQuery.id}", factory = PrivateSendViewModel.Factory(token))

/**
 * The toggle card, rendered on a send screen between the balance row and the memo/button
 * area. Not rendered at all for an unsupported token, not merely disabled. With the toggle
 * on, the amount field means "the amount the recipient receives".
 */
@Composable
fun PrivateSendToggleSection(viewModel: PrivateSendViewModel) {
    if (!viewModel.isSupported) return

    VSpacer(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        RowUniversal(
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { viewModel.onToggle(!viewModel.isEnabled) }
            ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp).weight(1f)) {
                body_leah(text = stringResource(R.string.PrivateSend_Toggle_Title))
                subhead2_grey(text = stringResource(R.string.PrivateSend_Toggle_Subtitle))
            }
            HsSwitch(
                modifier = Modifier.padding(end = 16.dp),
                checked = viewModel.isEnabled,
                onCheckedChange = { viewModel.onToggle(it) }
            )
        }
    }
}
