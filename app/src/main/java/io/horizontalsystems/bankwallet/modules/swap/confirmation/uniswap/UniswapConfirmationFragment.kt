package io.horizontalsystems.bankwallet.modules.swap.confirmation.uniswap

import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule
import io.horizontalsystems.bankwallet.modules.swap.confirmation.BaseSwapConfirmationFragment
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData

class UniswapConfirmationFragment(
    override val navGraphId: Int = R.id.uniswapConfirmationFragment
) : BaseSwapConfirmationFragment() {

    companion object {
        private const val transactionDataKey = "transactionDataKey"
        private const val dexKey = "dexKey"
        private const val additionalInfoKey = "additionalInfoKey"

        fun prepareParams(
            dex: SwapMainModule.Dex,
            transactionData: SendEvmModule.TransactionDataParcelable,
            additionalInfo: SendEvmData.AdditionalInfo?
        ) = bundleOf(
            dexKey to dex,
            transactionDataKey to transactionData,
            additionalInfoKey to additionalInfo
        )
    }

    private val dex by lazy {
        requireArguments().getParcelable<SwapMainModule.Dex>(dexKey)!!
    }

    private val transactionData by lazy {
        val transactionDataParcelable = requireArguments().getParcelable<SendEvmModule.TransactionDataParcelable>(transactionDataKey)!!
        TransactionData(
            Address(transactionDataParcelable.toAddress),
            transactionDataParcelable.value,
            transactionDataParcelable.input
        )
    }

    private val additionalInfo by lazy {
        requireArguments().getParcelable<SendEvmData.AdditionalInfo>(additionalInfoKey)
    }

    override val logger = AppLogger("swap_uniswap")

    private val vmFactory by lazy {
        UniswapConfirmationModule.Factory(
            dex,
            transactionData,
            additionalInfo
        )
    }
    override val sendEvmTransactionViewModel by navGraphViewModels<SendEvmTransactionViewModel>(navGraphId) { vmFactory }
    override val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(navGraphId) { vmFactory }
    override val nonceViewModel by navGraphViewModels<SendEvmNonceViewModel>(navGraphId) { vmFactory }

}
