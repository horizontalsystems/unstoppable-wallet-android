package bitcoin.wallet.core

import bitcoin.wallet.blockchain.BlockchainStorage
import bitcoin.wallet.entities.ExchangeRate

object ExchangeRateService {

    fun start(storage: BlockchainStorage) {
        hashMapOf(
                "BTC" to 6700.0,
                "ETH" to 500.0,
                "BCH" to 1000.0
        ).forEach {
            storage.updateExchangeRate(
                    ExchangeRate().apply {
                        code = it.key
                        value = it.value
                    }
            )
        }
    }

}
