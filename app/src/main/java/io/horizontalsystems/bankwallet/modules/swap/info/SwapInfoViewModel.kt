package io.horizontalsystems.bankwallet.modules.swap.info

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.StringProvider
import io.horizontalsystems.bankwallet.modules.swap.SwapModule

class SwapInfoViewModel(
        dex: SwapModule.Dex,
        stringProvider: StringProvider
) : ViewModel() {

    private val dexName = when (dex) {
        SwapModule.Dex.Uniswap -> "Uniswap"
        SwapModule.Dex.PancakeSwap -> "PancakeSwap"
    }

    private val blockchain = when (dex) {
        SwapModule.Dex.Uniswap -> "Ethereum"
        SwapModule.Dex.PancakeSwap -> "Binance Smart Chain"
    }

    val dexUrl = when (dex) {
        SwapModule.Dex.Uniswap -> "https://uniswap.org/"
        SwapModule.Dex.PancakeSwap -> "https://pancakeswap.finance/"
    }

    val title: String = when (dex) {
        SwapModule.Dex.Uniswap -> "Uniswap v.2"
        SwapModule.Dex.PancakeSwap -> "PancakeSwap"
    }

    val description = stringProvider.string(R.string.SwapInfo_Description, dexName, blockchain, dexName)

    val dexRelated = stringProvider.string(R.string.SwapInfo_DexRelated, dexName)

    val transactionFeeDescription = stringProvider.string(R.string.SwapInfo_TransactionFeeDescription, blockchain, dexName)

    val linkText = stringProvider.string(R.string.SwapInfo_Site, dexName)

}
