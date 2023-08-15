package cash.p.terminal.modules.swap.confirmation.uniswap

import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.send.evm.SendEvmData
import cash.p.terminal.modules.send.evm.SendEvmModule
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.confirmation.BaseSwapConfirmationFragment
import io.horizontalsystems.core.parcelable
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
        requireArguments().parcelable<SwapMainModule.Dex>(dexKey)!!
    }

    private val transactionData by lazy {
        val transactionDataParcelable = requireArguments().parcelable<SendEvmModule.TransactionDataParcelable>(transactionDataKey)!!
        TransactionData(
            Address(transactionDataParcelable.toAddress),
            transactionDataParcelable.value,
            transactionDataParcelable.input
        )
    }

    private val additionalInfo by lazy {
        requireArguments().parcelable<SendEvmData.AdditionalInfo>(additionalInfoKey)
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
