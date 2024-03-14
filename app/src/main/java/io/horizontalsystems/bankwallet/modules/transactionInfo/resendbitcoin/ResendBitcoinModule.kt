package cash.p.terminal.modules.transactionInfo.resendbitcoin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.adapters.BitcoinBaseAdapter
import cash.p.terminal.core.factories.FeeRateProviderFactory
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import cash.p.terminal.modules.transactionInfo.options.TransactionInfoOptionsModule
import cash.p.terminal.modules.transactions.TransactionSource
import cash.p.terminal.modules.xrate.XRateService

object ResendBitcoinModule {

    class Factory(
        private val optionType: TransactionInfoOptionsModule.Type,
        private val transactionRecord: BitcoinOutgoingTransactionRecord,
        private val source: TransactionSource
    ) : ViewModelProvider.Factory {

        private val adapter by lazy {
            App.transactionAdapterManager.getAdapter(source) as BitcoinBaseAdapter
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val replacementInfo = when (optionType) {
                TransactionInfoOptionsModule.Type.SpeedUp -> adapter.speedUpTransactionInfo(transactionRecord.transactionHash)
                TransactionInfoOptionsModule.Type.Cancel -> adapter.cancelTransactionInfo(transactionRecord.transactionHash)
            } ?: throw IllegalStateException("Wrong transaction")

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
