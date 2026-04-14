package com.quantum.wallet.bankwallet.modules.transactionInfo.resendbitcoin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.adapters.BitcoinBaseAdapter
import com.quantum.wallet.bankwallet.core.factories.FeeRateProviderFactory
import com.quantum.wallet.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import com.quantum.wallet.bankwallet.modules.transactionInfo.options.SpeedUpCancelType
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import com.quantum.wallet.bankwallet.modules.xrate.XRateService

object ResendBitcoinModule {

    class Factory(
        private val optionType: SpeedUpCancelType,
        private val transactionRecord: BitcoinOutgoingTransactionRecord,
        private val source: TransactionSource
    ) : ViewModelProvider.Factory {

        private val adapter by lazy {
            App.transactionAdapterManager.getAdapter(source) as BitcoinBaseAdapter
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val replacementInfo = when (optionType) {
                SpeedUpCancelType.SpeedUp -> adapter.speedUpTransactionInfo(transactionRecord.transactionHash)
                SpeedUpCancelType.Cancel -> adapter.cancelTransactionInfo(transactionRecord.transactionHash)
            }

            return ResendBitcoinViewModel(
                type = optionType,
                transactionRecord = transactionRecord,
                replacementInfo = replacementInfo,
                adapter = adapter,
                xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency),
                feeRateProvider =  FeeRateProviderFactory.provider(adapter.wallet.token.blockchainType)!!,
                contactsRepo = App.contactsRepository
            ) as T
        }
    }

}
