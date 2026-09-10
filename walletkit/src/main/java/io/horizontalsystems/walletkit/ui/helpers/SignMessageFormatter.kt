package io.horizontalsystems.walletkit.ui.helpers

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import java.io.StringReader

/**
 * Formats a message the user is about to sign for display. The signer always receives the
 * original text; this only decides how it is shown — so it must never show something the
 * original does not say. JSON is pretty-printed only when it re-serialises faithfully: a payload
 * with duplicate member names is shown verbatim, because a parser keeps one member and drops the
 * other, and the signer's parser may not keep the same one.
 */
object SignMessageFormatter {

    private val prettyJson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping() // keep "<", "&", "=" readable instead of \u003c
        .serializeNulls()      // a null the dApp sent is part of what is signed; do not hide it
        .create()

    // EIP-712 payloads as dApps build them start with `types`: a long list of field names paired
    // with Solidity types and no values, which is what the user first sees and reads as empty
    // fields. The values they are signing (`message`) and what binds the signature (`domain`)
    // come after — put them first and keep `types` last. Key order does not affect what is signed.
    private val typedDataKeyOrder = listOf("primaryType", "message", "domain", "types")

    fun format(text: String): String = try {
        if (hasDuplicateMemberNames(text)) {
            text
        } else {
            val element = JsonParser.parseString(text)
            when {
                element.isJsonObject -> prettyJson.toJson(withTypedDataValuesFirst(element.asJsonObject))
                element.isJsonArray -> prettyJson.toJson(element)
                else -> text
            }
        }
    } catch (e: Exception) {
        text
    }

    private fun withTypedDataValuesFirst(json: JsonObject): JsonObject {
        if (!json.has("types") || !json.has("message")) return json

        val reordered = JsonObject()
        typedDataKeyOrder.forEach { key -> json.get(key)?.let { reordered.add(key, it) } }
        json.entrySet().forEach { (key, value) -> if (!reordered.has(key)) reordered.add(key, value) }
        return reordered
    }

    /**
     * Streams the JSON and reports whether any object declares the same member name twice.
     * Throws on malformed input, which the caller treats as "not JSON".
     */
    private fun hasDuplicateMemberNames(text: String): Boolean {
        val reader = JsonReader(StringReader(text))
        val names = ArrayDeque<MutableSet<String>>()

        while (true) {
            when (reader.peek()) {
                JsonToken.BEGIN_OBJECT -> {
                    reader.beginObject()
                    names.addLast(mutableSetOf())
                }

                JsonToken.END_OBJECT -> {
                    reader.endObject()
                    names.removeLast()
                }

                JsonToken.NAME -> if (!names.last().add(reader.nextName())) return true
                JsonToken.BEGIN_ARRAY -> reader.beginArray()
                JsonToken.END_ARRAY -> reader.endArray()
                JsonToken.END_DOCUMENT -> return false
                else -> reader.skipValue()
            }
        }
    }
}
