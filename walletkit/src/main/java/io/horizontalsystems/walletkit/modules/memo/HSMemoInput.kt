package io.horizontalsystems.walletkit.modules.memo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.compose.ColoredTextStyle
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.compose.components.HsDivider
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

/**
 * A full-width memo cell: a single-line field closed by a divider, with the memo's
 * visibility explained in a caption below. Sits directly under the row above it, which
 * provides the top divider.
 */
@Composable
fun HSMemoInput(
    maxLength: Int,
    memo: String? = null,
    visibility: MemoVisibility = MemoVisibility.Public,
    // Shown as the caption while there is typed text — the flow's explanation of why the
    // memo will not be attached to the transaction right now.
    onValueChange: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf(memo ?: "") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                onValueChange = { value ->
                    if (value.length <= maxLength) {
                        text = value
                        onValueChange(value)
                    }
                },
                singleLine = true,
                textStyle = ColoredTextStyle(
                    color = ComposeAppTheme.colors.leah,
                    textStyle = ComposeAppTheme.typography.body,
                ),
                cursorBrush = SolidColor(ComposeAppTheme.colors.leah),
                decorationBox = { innerTextField ->
                    if (text.isEmpty()) {
                        Text(
                            text = stringResource(R.string.Send_DialogMemoHint),
                            style = ComposeAppTheme.typography.body,
                            color = ComposeAppTheme.colors.andy,
                        )
                    }
                    innerTextField()
                },
            )
        }
        HsDivider(modifier = Modifier.fillMaxWidth())

        val infoText = when (visibility) {
            MemoVisibility.Encrypted -> stringResource(R.string.Send_Memo_EncryptedInfo)
            MemoVisibility.Offchain -> stringResource(R.string.Send_Memo_OffchainInfo)
            MemoVisibility.Public -> stringResource(R.string.Send_Memo_PublicWarning)
        }
        caption_grey(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp), text = infoText)
    }
}
