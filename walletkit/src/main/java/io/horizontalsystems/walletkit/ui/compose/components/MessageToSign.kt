package io.horizontalsystems.walletkit.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.helpers.SignMessageFormatter
import io.horizontalsystems.walletkit.ui.helpers.TextHelper

@Composable
fun MessageToSign(
    message: String,
    onCopy: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val formatted = SignMessageFormatter.format(message)
    val copyMessage = stringResource(R.string.Hud_Text_Copied)

    body_leah(
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) {
            TextHelper.copyText(formatted)
            onCopy.invoke(copyMessage)
        },
        text = formatted
    )
}
