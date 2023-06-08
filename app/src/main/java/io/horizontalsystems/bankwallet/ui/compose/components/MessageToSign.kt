package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.helpers.HudHelper

@Composable
fun MessageToSign(message: String) {
    val localView = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }

    VSpacer(24.dp)
    HeaderText(text = stringResource(id = R.string.WalletConnect_SignMessageRequest_ShowMessageTitle).uppercase())
    CellUniversalLawrenceSection(buildList {
        add {
            val formatted = formatJson(message)
            caption_leah(
                modifier = Modifier
                    .padding(16.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        TextHelper.copyText(formatted)
                        HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)
                    },
                text = formatted
            )
        }
    })
}

private fun formatJson(text: String): String {
    val json = StringBuilder()
    var indentString = ""
    for (element in text) {
        when (element) {
            '{', '[' -> {
                json.append("\n$indentString$element\n")
                indentString += "\t"
                json.append(indentString)
            }

            '}', ']' -> {
                indentString = indentString.replaceFirst("\t".toRegex(), "")
                json.append("\n$indentString$element")
            }

            ',' -> json.append("$element\n$indentString")
            else -> json.append(element)
        }
    }
    return json.toString()
}
