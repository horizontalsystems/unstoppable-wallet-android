package io.horizontalsystems.bankwallet.serializers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DAppRequestEntitySerializer : KSerializer<DAppRequestEntity> {
    override val descriptor = PrimitiveSerialDescriptor("DAppRequestEntity", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DAppRequestEntity) {
        val json = JsonObject().apply {
            addProperty("v", value.v)
            addProperty("id", value.id)
            addProperty("r", value.r)
            value.ret?.let { addProperty("ret", it) }
        }
        encoder.encodeString(json.toString())
    }

    override fun deserialize(decoder: Decoder): DAppRequestEntity {
        val json = JsonParser.parseString(decoder.decodeString()).asJsonObject
        return DAppRequestEntity(
            v = json.get("v").asInt,
            id = json.get("id").asString,
            r = json.get("r").asString,
            ret = json.get("ret")?.asString
        )
    }
}
