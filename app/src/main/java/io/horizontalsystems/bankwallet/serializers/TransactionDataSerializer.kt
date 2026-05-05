package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigInteger

object TransactionDataSerializer : KSerializer<TransactionData> {
    override val descriptor = PrimitiveSerialDescriptor("TransactionData", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: TransactionData) {
        val inputHex = value.input.joinToString("") { "%02x".format(it) }
        encoder.encodeString("${value.to.hex}|${value.value}|$inputHex")
    }

    override fun deserialize(decoder: Decoder): TransactionData {
        val parts = decoder.decodeString().split("|", limit = 3)
        val input = if (parts[2].isEmpty()) byteArrayOf()
        else parts[2].chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return TransactionData(
            to = Address(parts[0]),
            value = BigInteger(parts[1]),
            input = input
        )
    }
}
