package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object AccountSerializer : KSerializer<Account> {
    override val descriptor = PrimitiveSerialDescriptor("Account", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Account) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): Account {
        return App.accountManager.account(decoder.decodeString())!!
    }
}
