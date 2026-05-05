package io.horizontalsystems.bankwallet.serializers

import com.google.gson.Gson
import io.horizontalsystems.marketkit.models.CoinCategory
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object CoinCategorySerializer : KSerializer<CoinCategory> {
    private val gson = Gson()

    override val descriptor = PrimitiveSerialDescriptor("CoinCategory", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: CoinCategory) {
        encoder.encodeString(gson.toJson(value))
    }

    override fun deserialize(decoder: Decoder): CoinCategory {
        return gson.fromJson(decoder.decodeString(), CoinCategory::class.java)
    }
}
