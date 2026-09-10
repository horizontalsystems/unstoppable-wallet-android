package io.horizontalsystems.walletkit.ui.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.ui.helpers.TextHelper

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

// Pretty-prints a JSON payload (typed data, a transaction object) so every key sits on the same
// line as its value. The previous character-by-character formatter put "types": / "domain": /
// "message": alone on a line with the object opening below, which read as a row of empty fields.
// Non-JSON text (a personal_sign message) is shown as is.
private val prettyJson: Gson = GsonBuilder()
    .setPrettyPrinting()
    .disableHtmlEscaping() // keep "<", "&", "=" readable instead of \u003c
    .serializeNulls()      // a null the dApp sent is part of what is signed; do not hide it
    .create()

private fun formatJson(text: String): String = try {
    val element = JsonParser.parseString(text)
    when {
        element.isJsonObject -> prettyJson.toJson(withTypedDataValuesFirst(element.asJsonObject))
        element.isJsonArray -> prettyJson.toJson(element)
        else -> text
    }
} catch (e: Exception) {
    text
}

// EIP-712 payloads as dApps build them start with `types`: a long list of field names paired
// with Solidity types and no values, which is what the user first sees and reads as empty fields.
// The values they are signing (`message`) and what binds the signature (`domain`) come after —
// put them first and keep `types` last. Key order does not affect what is signed.
private val typedDataKeyOrder = listOf("primaryType", "message", "domain", "types")

private fun withTypedDataValuesFirst(json: JsonObject): JsonObject {
    if (!json.has("types") || !json.has("message")) return json

    val reordered = JsonObject()
    typedDataKeyOrder.forEach { key -> json.get(key)?.let { reordered.add(key, it) } }
    json.entrySet().forEach { (key, value) -> if (!reordered.has(key)) reordered.add(key, value) }
    return reordered
}
