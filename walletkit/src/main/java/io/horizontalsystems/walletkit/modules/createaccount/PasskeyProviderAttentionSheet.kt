package io.horizontalsystems.walletkit.modules.createaccount

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.InfoTextBody
import io.horizontalsystems.walletkit.ui.extensions.HSBottomSheet
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetContent
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.ButtonsStack
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import kotlinx.serialization.Serializable

/**
 * Shown when the passkey ceremony fails inside the credential provider itself
 * (its vault is locked, a verification is pending, ...). The default wording
 * stays provider-neutral: the failure envelope is the same for every provider
 * and the app never learns which one the user picked in the system sheet. The
 * one recognizable signature — Google Password Manager's sync-passphrase
 * lock — gets its own concrete steps.
 */
@Serializable
data class PasskeyProviderAttentionSheet(val gpmPassphrase: Boolean) : HSBottomSheet() {

    companion object {
        // The "[11000] Passphrase required" envelope is Play-services-styled and
        // Google Password Manager is the only provider with a sync-passphrase
        // vault lock, so the message alone identifies the case.
        fun isGpmPassphrase(e: Throwable): Boolean =
            e.message?.contains("passphrase", ignoreCase = true) == true
    }

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        PasskeyProviderAttentionScreen(navigation, gpmPassphrase)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasskeyProviderAttentionScreen(navigation: HSNavigation, gpmPassphrase: Boolean) {
    BottomSheetContent(
        onDismissRequest = navigation::removeLastOrNull,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        BottomSheetHeaderV3(
            image72 = painterResource(R.drawable.warning_filled_24),
            imageTint = ComposeAppTheme.colors.jacob,
            title = stringResource(R.string.Passkey_ProviderAttention_Title)
        )
        InfoTextBody(
            text = stringResource(
                if (gpmPassphrase) {
                    R.string.Passkey_ProviderAttention_Gpm_Description
                } else {
                    R.string.Passkey_ProviderAttention_Description
                }
            ),
            color = ComposeAppTheme.colors.grey,
            textAlign = TextAlign.Center
        )
        ButtonsStack {
            HSButton(
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                title = stringResource(R.string.Button_Ok)
            ) {
                navigation.removeLastOrNull()
            }
        }
    }
}
