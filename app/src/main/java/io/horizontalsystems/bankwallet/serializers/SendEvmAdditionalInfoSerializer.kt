package io.horizontalsystems.bankwallet.serializers

import com.google.gson.Gson
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object SendEvmAdditionalInfoSerializer : KSerializer<SendEvmData.AdditionalInfo> {
    private val gson = Gson()

    override val descriptor = PrimitiveSerialDescriptor("SendEvmAdditionalInfo", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: SendEvmData.AdditionalInfo) {
        val encoded = when (value) {
            is SendEvmData.AdditionalInfo.Send -> "Send|${gson.toJson(value.info)}"
        }
        encoder.encodeString(encoded)
    }

    override fun deserialize(decoder: Decoder): SendEvmData.AdditionalInfo {
        val encoded = decoder.decodeString()
        val separatorIdx = encoded.indexOf('|')
        val type = encoded.substring(0, separatorIdx)
        val data = encoded.substring(separatorIdx + 1)
        return when (type) {
            "Send" -> {
                val info = gson.fromJson(data, SendEvmData.SendInfo::class.java)
                SendEvmData.AdditionalInfo.Send(info)
            }
            else -> throw IllegalArgumentException("Unknown AdditionalInfo type: $type")
        }
    }
}
