package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.bankwallet.core.customCoinUid
import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.Address
import io.horizontalsystems.tonkit.core.TonKit
import io.horizontalsystems.tonkit.models.Network

class AddTonTokenBlockchainService(private val blockchain: Blockchain) : AddTokenModule.IAddTokenBlockchainService {
    override fun isValid(reference: String) = try {
        TonKit.validateAddress(reference)
        true
    } catch (e: Throwable) {
        false
    }

    override fun tokenQuery(reference: String): TokenQuery {
        return TokenQuery(BlockchainType.Ton, TokenType.Jetton(reference))
    }

    override suspend fun token(reference: String): Token {
        val jetton = TonKit.getJetton(Network.MainNet, Address.parse(reference))

        val tokenQuery = tokenQuery(reference)
        return Token(
            coin = Coin(
                uid = tokenQuery.customCoinUid,
                name = jetton.name,
                code = jetton.symbol,
                image = jetton.image
            ),
            blockchain = blockchain,
            type = tokenQuery.tokenType,
            decimals = jetton.decimals
        )


    }

}