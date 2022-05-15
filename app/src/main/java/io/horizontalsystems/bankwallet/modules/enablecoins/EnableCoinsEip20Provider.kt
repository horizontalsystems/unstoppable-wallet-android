package io.horizontalsystems.bankwallet.modules.enablecoins

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.ethereumkit.core.hexStringToLongOrNull
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.Single

class EnableCoinsEip20Provider(
    private val networkManager: INetworkManager,
    private val mode: EnableCoinMode,
    private val apiKey: String
) {

    private val url: String
        get() = when (mode) {
            EnableCoinMode.Erc20 -> "https://api.etherscan.io/"
            EnableCoinMode.Bep20 -> "https://api.bscscan.com/"
        }

    private fun coinType(address: String): CoinType {
        return when (mode) {
            EnableCoinMode.Erc20 -> CoinType.Erc20(address)
            EnableCoinMode.Bep20 -> CoinType.Bep20(address)
        }
    }

    private fun coinTypes(contractAddresses: List<String>): List<CoinType> {
        return contractAddresses.map { coinType(it) }
    }

    fun getCoinTypesAsync(address: String): Single<List<CoinType>> {
        val params = "api?module=account&action=tokentx&sort=asc&address=$address"
        val gson = Gson()

        return networkManager.getEvmInfo(url, params)
                .map { jsonObject ->
                    if (jsonObject.get("status").asString == "1" && jsonObject.has("result")) {
                        val type = object : TypeToken<ArrayList<Transaction>>() {}.type
                        val transactions = gson.fromJson<ArrayList<Transaction>>(jsonObject.get("result"), type).distinct()
                        coinTypes(transactions.map { it.contractAddress }.distinct())
                    } else {
                        listOf()
                    }
                }
    }

    fun getCoinTypesAsync(address: String, startBlock: Long = 0): Single<Pair<List<CoinType>, Long>> {
        val paramsTxs = "api?module=account&action=tokentx&sort=asc&address=$address&startBlock=$startBlock&apikey=$apiKey"
        val gson = Gson()

        val coinTypesSingle = networkManager.getEvmInfo(url, paramsTxs)
            .map { jsonObject ->
                if (jsonObject.get("status").asString == "1" && jsonObject.has("result")) {
                    val type = object : TypeToken<ArrayList<Transaction>>() {}.type
                    val transactions = gson.fromJson<ArrayList<Transaction>>(jsonObject.get("result"), type).distinct()
                    coinTypes(transactions.map { it.contractAddress }.distinct())
                } else {
                    listOf()
                }
            }

        val paramsBlockNumber = "api?module=proxy&action=eth_blockNumber&apikey=$apiKey"
        val blockNumberSingle = networkManager.getEvmInfo(url, paramsBlockNumber)
            .map {
                it.get("result").asString.hexStringToLongOrNull() ?: 0
            }

        return blockNumberSingle.zipWith(coinTypesSingle, { blockNumber, coinTypes ->
            Pair(coinTypes, blockNumber)
        })
    }

    enum class EnableCoinMode {
        Erc20, Bep20
    }

    data class Transaction(
            val blockNumber: Long,
            val contractAddress: String,
            val tokenName: String,
            val tokenSymbol: String,
            val tokenDecimal: String
    )

}
