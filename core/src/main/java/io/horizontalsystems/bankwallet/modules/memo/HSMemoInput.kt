package io.horizontalsystems.bankwallet.modules.memo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInput
import io.horizontalsystems.bankwallet.ui.compose.components.FormsInputStateWarning
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey

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
    onValueChange: (String) -> Unit
) {
    val state = when (visibility) {
        MemoVisibility.Public -> DataState.Error(
            FormsInputStateWarning(stringResource(R.string.Send_Memo_PublicWarning))
        )

        MemoVisibility.Encrypted,
        MemoVisibility.Offchain -> null
    }

    val infoText = when (visibility) {
        MemoVisibility.Public -> null
        MemoVisibility.Encrypted -> stringResource(R.string.Send_Memo_EncryptedInfo)
        MemoVisibility.Offchain -> stringResource(R.string.Send_Memo_OffchainInfo)
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        FormsInput(
            hint = stringResource(R.string.Send_DialogMemoHint),
            initial = memo,
            hintColor = ComposeAppTheme.colors.andy,
            hintStyle = ComposeAppTheme.typography.bodyItalic,
            textColor = ComposeAppTheme.colors.leah,
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
