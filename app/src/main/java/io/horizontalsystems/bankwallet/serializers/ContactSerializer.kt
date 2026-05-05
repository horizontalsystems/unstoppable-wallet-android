package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ContactSerializer : KSerializer<Contact> {
    override val descriptor = PrimitiveSerialDescriptor("Contact", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Contact) {
        encoder.encodeString(value.uid)
    }

    override fun deserialize(decoder: Decoder): Contact {
        val uid = decoder.decodeString()
        return App.contactsRepository.contacts.find { it.uid == uid }
            ?: error("Contact not found: $uid")
    }
}
