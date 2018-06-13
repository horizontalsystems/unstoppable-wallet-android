package org.grouvi.wallet.core

import com.google.gson.JsonParser
import io.reactivex.Flowable
import org.grouvi.wallet.blockchain.info.BlockchainInfoClient
import org.grouvi.wallet.entities.Transaction
import org.grouvi.wallet.entities.TransactionInput
import org.grouvi.wallet.entities.TransactionOutput
import org.grouvi.wallet.entities.UnspentOutput
import org.grouvi.wallet.lib.WalletDataManager

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
                    JsonParser().parse(it.string()).asJsonObject["unspent_outputs"].asJsonArray.map {
                        UnspentOutput(it.asJsonObject["value"].asLong)
                    }
                }

    }
}