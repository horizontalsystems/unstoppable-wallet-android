package io.horizontalsystems.bankwallet.serializers

import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DAppRequestEntitySerializer : KSerializer<DAppRequestEntity> {
    override val descriptor: SerialDescriptor
        get() = TODO("Not yet implemented")

    override fun serialize(
        encoder: Encoder,
        value: DAppRequestEntity
    ) {
        TODO("Not yet implemented")
    }

    override fun deserialize(decoder: Decoder): DAppRequestEntity {
        TODO("Not yet implemented")
    }

}
