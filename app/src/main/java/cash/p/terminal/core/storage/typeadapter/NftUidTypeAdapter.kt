package cash.p.terminal.core.storage.typeadapter

import cash.p.terminal.entities.nft.NftUid
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

/**
 * Custom type adapter for BlockchainType sealed class
 */
class NftUidTypeAdapter : JsonSerializer<NftUid>, JsonDeserializer<NftUid> {

    override fun serialize(
        src: NftUid,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        // Create a JSON object with uid property
        val jsonObject = JsonObject()
        jsonObject.addProperty("uid", src.uid)
        return jsonObject
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): NftUid {
        // Extract uid from JSON
        val uid = when {
            json.isJsonObject -> json.asJsonObject.get("uid")?.asString
            json.isJsonPrimitive -> json.asString
            else -> null
        } ?: throw IllegalArgumentException("Cannot deserialize cash.p.terminal.entities.nft.NftUid: invalid format")

        // Convert uid to cash.p.terminal.entities.nft.NftUid
        return NftUid.fromUid(uid)
    }
}