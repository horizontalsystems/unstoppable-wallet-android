package io.horizontalsystems.walletkit.modules.memo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.FormsInput
import io.horizontalsystems.walletkit.ui.compose.components.FormsInputStateWarning
import io.horizontalsystems.walletkit.ui.compose.components.caption_grey

/**
 * Describes where a memo ends up once the transaction is sent, so the UI can
 * communicate the privacy implications consistently across blockchains.
 *
 * - [Public]: written on-chain in clear text (e.g. Bitcoin OP_RETURN, Stellar, TON) — anyone can read it.
 * - [Encrypted]: written on-chain but encrypted, readable only by sender and recipient (e.g. Zcash shielded).
 * - [Offchain]: kept only on this device, never broadcast to the blockchain, and not recovered on wallet
 *   restore (e.g. Monero, Zano).
 */
enum class MemoVisibility {
    Public,
    Encrypted,
    Offchain
}

@Composable
fun HSMemoInput(
    maxLength: Int,
    memo: String? = null,
    visibility: MemoVisibility = MemoVisibility.Public,
    enabled: Boolean = true,
    // Rendered as the input's (red) caution while [enabled] is false — the flow's
    // explanation of why a memo is not accepted right now. Any already-typed text stays
    // visible, greyed, and comes back editable when the input is re-enabled.
    disabledCaution: String? = null,
    onValueChange: (String) -> Unit
) {
    val state = when {
        !enabled -> disabledCaution?.let { DataState.Error(Exception(it)) }

        visibility == MemoVisibility.Public -> DataState.Error(
            FormsInputStateWarning(stringResource(R.string.Send_Memo_PublicWarning))
        )

        else -> null
    }

    val infoText = when {
        !enabled -> null
        visibility == MemoVisibility.Encrypted -> stringResource(R.string.Send_Memo_EncryptedInfo)
        visibility == MemoVisibility.Offchain -> stringResource(R.string.Send_Memo_OffchainInfo)
        else -> null
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        FormsInput(
            hint = stringResource(R.string.Send_DialogMemoHint),
            initial = memo,
            enabled = enabled,
            hintColor = ComposeAppTheme.colors.andy,
            hintStyle = ComposeAppTheme.typography.bodyItalic,
            textColor = if (enabled) ComposeAppTheme.colors.leah else ComposeAppTheme.colors.andy,
            textStyle = ComposeAppTheme.typography.bodyItalic,
            pasteEnabled = false,
            singleLine = true,
            maxLength = maxLength,
            state = state,
            onValueChange = onValueChange
        )

        infoText?.let {
            caption_grey(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
                text = it
            )
        }
    }
}
