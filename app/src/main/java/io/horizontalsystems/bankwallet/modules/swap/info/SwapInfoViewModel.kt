package io.horizontalsystems.bankwallet.modules.swap.info

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule

class SwapInfoViewModel(
        dex: SwapMainModule.Dex
) : ViewModel() {

    private val dexName = dex.provider.title

    private val blockchain = dex.blockchain.title

    val title = dex.provider.title

    val dexUrl = dex.provider.url

    val description = Translator.getString(R.string.SwapInfo_Description, dexName, blockchain, dexName)

    val dexRelated = Translator.getString(R.string.SwapInfo_DexRelated, dexName)

    val transactionFeeDescription = Translator.getString(R.string.SwapInfo_TransactionFeeDescription, blockchain, dexName)

    val linkText = Translator.getString(R.string.SwapInfo_Site, dexName)

}
