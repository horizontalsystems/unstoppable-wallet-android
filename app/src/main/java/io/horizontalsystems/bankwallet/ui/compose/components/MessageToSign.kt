package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper

@Composable
fun MessageToSign(
    message: String,
    onCopy: (String) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val formatted = formatJson(message)
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
