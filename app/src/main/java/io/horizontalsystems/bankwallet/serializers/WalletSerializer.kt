package io.horizontalsystems.bankwallet.serializers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object WalletSerializer : KSerializer<Wallet> {
    override val descriptor = PrimitiveSerialDescriptor("Wallet", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Wallet) {
        encoder.encodeString("${value.account.id}|${value.token.tokenQuery.id}")
    }

    override fun deserialize(decoder: Decoder): Wallet {
        val encoded = decoder.decodeString()
        val separatorIdx = encoded.indexOf('|')
        val accountId = encoded.substring(0, separatorIdx)
        val tokenQueryId = encoded.substring(separatorIdx + 1)
        val account = App.accountManager.account(accountId)!!
        val tokenQuery = TokenQuery.fromId(tokenQueryId)!!
        val token = App.coinManager.getToken(tokenQuery)!!
        return Wallet(token, account)
    }
}
