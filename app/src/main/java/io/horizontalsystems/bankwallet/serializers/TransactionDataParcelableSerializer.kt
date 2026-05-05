package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigInteger

object TransactionDataParcelableSerializer : KSerializer<SendEvmModule.TransactionDataParcelable> {
    override val descriptor = PrimitiveSerialDescriptor("TransactionDataParcelable", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SendEvmModule.TransactionDataParcelable) {
        val inputHex = value.input.joinToString("") { "%02x".format(it) }
        encoder.encodeString("${value.toAddress}|${value.value}|$inputHex")
    }

    override fun deserialize(decoder: Decoder): SendEvmModule.TransactionDataParcelable {
        val parts = decoder.decodeString().split("|", limit = 3)
        val input = if (parts[2].isEmpty()) byteArrayOf()
        else parts[2].chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        return SendEvmModule.TransactionDataParcelable(
            toAddress = parts[0],
            value = BigInteger(parts[1]),
            input = input
        )
    }
}
