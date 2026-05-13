package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

object HSScreenKClassSerializer : KSerializer<KClass<out HSScreen>> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("KClass<HSScreen>", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KClass<out HSScreen>) {
        encoder.encodeString(value.qualifiedName!!)
    }

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(decoder: Decoder): KClass<out HSScreen> {
        return Class.forName(decoder.decodeString()).kotlin as KClass<out HSScreen>
    }
}
