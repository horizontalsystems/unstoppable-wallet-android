package bitcoin.wallet.core

import bitcoin.wallet.blockchain.info.BlockchainInfoClient
import bitcoin.wallet.entities.*
import bitcoin.wallet.lib.WalletDataManager
import com.google.gson.JsonParser
import io.reactivex.Flowable

class NetworkManager : INetworkManager {
    fun getTransactions(): Flowable<List<Transaction>> {

//        val pkey = "tpubDDf1ySGMy5TDPQjy5KScHgvu1mWCCWvf28ydrNDWmsNkzjb5UCmjHEvW7NqFi7cvUnY4FdAbD2H5wCjZRKBMt5VkDanPCX2W8fL17srC5xN"
//        return GrouviApi.service.transactions(pkey)

        return BlockchainInfoClient.service.multiaddr(WalletDataManager.getAddresses().joinToString("|"))
                .map {
                    JsonParser().parse(it.string()).asJsonObject["txs"].asJsonArray.map {

                        val transactionObject = it.asJsonObject

                        Transaction().apply {

                            timestamp = transactionObject["time"].asLong * 1000

                            inputs = transactionObject["inputs"].asJsonArray.map {
                                val prevOut = it.asJsonObject["prev_out"].asJsonObject

                                TransactionInput(prevOut["addr"].asString, prevOut["value"].asLong)
                            }

                            outputs = transactionObject["out"].asJsonArray.map {
                                val out = it.asJsonObject

                                TransactionOutput(out["addr"].asString, out["value"].asLong)
                            }
                        }
                    }
                }

    }

    override fun getUnspentOutputs(): Flowable<List<UnspentOutput>> {
        return BlockchainInfoClient.service.unspent(WalletDataManager.getAddresses().joinToString("|"))
                .map {
                    it.unspentOutputs
                }

    }

    override fun getExchangeRates(): Flowable<List<ExchangeRate>> {
        val exchangeRate = ExchangeRate().apply {
            code = "BTC"
            value = 7200.0
        }
        return Flowable.just(listOf(exchangeRate))
    }

}
