package io.horizontalsystems.bankwallet.modules.enablecoins

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.Single

class EnableCoinsEip20Provider(
        private val networkManager: INetworkManager,
        private val appConfigProvider: IAppConfigProvider,
        private val mode: EnableCoinMode
) {

    private val url: String
        get() = when (mode) {
            EnableCoinMode.Erc20 -> "https://api.etherscan.io/"
            EnableCoinMode.Bep20 -> "https://api.bscscan.com/"
        }

    private val apiKey: String
        get() = when (mode) {
            EnableCoinMode.Erc20 -> appConfigProvider.etherscanApiKey
            EnableCoinMode.Bep20 -> appConfigProvider.bscscanApiKey
        }

    private fun coinType(address: String): CoinType {
        return when (mode) {
            EnableCoinMode.Erc20 -> CoinType.Erc20(address)
            EnableCoinMode.Bep20 -> CoinType.Bep20(address)
        }
    }

    private fun coinTypes(transactions: List<Transaction>): List<CoinType> {
        return transactions.map { coinType(it.contractAddress) }
    }

    fun getCoinTypesAsync(address: String): Single<List<CoinType>> {
        val params = "api?module=account&action=tokentx&sort=asc&address=$address&apikey=$apiKey"
        val gson = Gson()

        return networkManager.getEvmInfo(url, params)
                .map { jsonObject ->
                    if (jsonObject.get("status").asString == "1" && jsonObject.has("result")) {
                        val type = object : TypeToken<ArrayList<Transaction>>() {}.type
                        val transactions = gson.fromJson<ArrayList<Transaction>>(jsonObject.get("result"), type).distinct()
                        coinTypes(transactions)
                    } else {
                        listOf()
                    }
                }
    }

    enum class EnableCoinMode {
        Erc20, Bep20
    }

    data class Transaction(
            val contractAddress: String,
            val tokenName: String,
            val tokenSymbol: String,
            val tokenDecimal: String
    )

}
