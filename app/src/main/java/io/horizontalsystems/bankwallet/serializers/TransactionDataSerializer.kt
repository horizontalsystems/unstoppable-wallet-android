package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object TransactionDataSerializer : KSerializer<TransactionData> {
    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun serialize(
        encoder: Encoder,
        value: TransactionData
    ) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): TransactionData {
        TODO("Not yet implemented")
    }

}