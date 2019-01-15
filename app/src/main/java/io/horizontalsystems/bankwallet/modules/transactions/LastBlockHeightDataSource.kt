package io.horizontalsystems.bankwallet.modules.transactions

class LastBlockHeightDataSource {

    private val lastBlockHeights = mutableMapOf<CoinCode, Int>()
    private val thresholds = mutableMapOf<CoinCode, Int>()

    fun setLastBlockHeight(lastBlockHeight: Int, coinCode: CoinCode) {
        lastBlockHeights[coinCode] = lastBlockHeight
    }

    fun getLastBlockHeight(coinCode: CoinCode): Int? =
            lastBlockHeights[coinCode]

    fun setConfirmationThreshold(threshold: Int, coinCode: CoinCode) {
        thresholds[coinCode] = threshold
    }

    fun getConfirmationThreshold(coinCode: CoinCode): Int? =
            thresholds[coinCode]

}
