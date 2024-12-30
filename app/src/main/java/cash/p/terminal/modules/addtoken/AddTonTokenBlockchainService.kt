package cash.p.terminal.modules.addtoken

import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.customCoinUid
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
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
        return cash.p.terminal.wallet.Token(
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