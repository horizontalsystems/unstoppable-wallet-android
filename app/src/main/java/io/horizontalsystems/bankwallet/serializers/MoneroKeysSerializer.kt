package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.bankwallet.modules.manageaccount.showmonerokey.ShowMoneroKeyModule
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object MoneroKeysSerializer : KSerializer<ShowMoneroKeyModule.MoneroKeys> {
    override val descriptor = PrimitiveSerialDescriptor("MoneroKeys", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ShowMoneroKeyModule.MoneroKeys) {
        encoder.encodeString("${value.spendKey}|${value.viewKey}|${value.isPrivate}")
    }

    override fun deserialize(decoder: Decoder): ShowMoneroKeyModule.MoneroKeys {
        val parts = decoder.decodeString().split("|", limit = 3)
        return ShowMoneroKeyModule.MoneroKeys(
            spendKey = parts[0],
            viewKey = parts[1],
            isPrivate = parts[2].toBoolean()
        )
    }
}
