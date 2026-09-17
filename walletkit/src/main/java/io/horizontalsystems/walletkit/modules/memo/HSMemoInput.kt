package io.horizontalsystems.walletkit.modules.memo

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.modules.send.v2.SendInputCell
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme

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

/** The memo cell of the send screen, with the memo's visibility explained in its caption. */
@Composable
fun HSMemoInput(
    maxLength: Int,
    memo: String? = null,
    visibility: MemoVisibility = MemoVisibility.Public,
    onValueChange: (String) -> Unit
) {
    SendInputCell(
        value = memo,
        hint = stringResource(R.string.Send_DialogMemoHint),
        enabled = true,
        keyboardType = KeyboardType.Text,
        maxLength = maxLength,
        caption = when (visibility) {
            MemoVisibility.Encrypted -> stringResource(R.string.Send_Memo_EncryptedInfo)
            MemoVisibility.Offchain -> stringResource(R.string.Send_Memo_OffchainInfo)
            MemoVisibility.Public -> stringResource(R.string.Send_Memo_PublicWarning)
        },
        captionColor = ComposeAppTheme.colors.grey,
        onValueChange = onValueChange,
    )
}
