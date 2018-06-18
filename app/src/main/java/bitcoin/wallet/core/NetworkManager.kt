package bitcoin.wallet.core

import bitcoin.wallet.blockchain.info.BlockchainInfoClient
import bitcoin.wallet.entities.Transaction
import bitcoin.wallet.entities.TransactionInput
import bitcoin.wallet.entities.TransactionOutput
import bitcoin.wallet.entities.UnspentOutput
import bitcoin.wallet.lib.WalletDataManager
import com.google.gson.JsonParser
import io.reactivex.Flowable

object NetworkManager {
    fun getTransactions(): Flowable<List<Transaction>> {
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

    fun getUnspentOutputs(): Flowable<List<UnspentOutput>> {
        return BlockchainInfoClient.service.unspent(WalletDataManager.getAddresses().joinToString("|"))
                .map {
                    it.unspentOutputs
                }

    }
}