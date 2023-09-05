package cash.p.terminal.modules.swap.confirmation.oneinch

import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.swap.SwapMainModule.OneInchSwapParameters
import cash.p.terminal.modules.swap.confirmation.BaseSwapConfirmationFragment
import io.horizontalsystems.core.parcelable
import io.horizontalsystems.marketkit.models.BlockchainType

class OneInchSwapConfirmationFragment(
    override val navGraphId: Int = R.id.oneInchConfirmationFragment
) : BaseSwapConfirmationFragment() {

    companion object {
        private const val blockchainTypeKey = "blockchainTypeKey"
        private const val oneInchSwapParametersKey = "oneInchSwapParametersKey"

        fun prepareParams(
            blockchainType: BlockchainType,
            oneInchSwapParameters: OneInchSwapParameters,
            swapEntryPointDestId: Int,
        ) = bundleOf(
            blockchainTypeKey to blockchainType,
            oneInchSwapParametersKey to oneInchSwapParameters,
            swapEntryPointDestIdKey to swapEntryPointDestId
        )
    }

    private val blockchainType by lazy {
        requireArguments().parcelable<BlockchainType>(blockchainTypeKey)!!
    }

    private val oneInchSwapParameters by lazy {
        requireArguments().parcelable<OneInchSwapParameters>(oneInchSwapParametersKey)!!
    }

    override val logger = AppLogger("swap_1inch")

    private val vmFactory by lazy {
        OneInchConfirmationModule.Factory(blockchainType, oneInchSwapParameters)
    }
    override val sendEvmTransactionViewModel by navGraphViewModels<SendEvmTransactionViewModel>(navGraphId) { vmFactory }
    override val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(navGraphId) { vmFactory }
    override val nonceViewModel by navGraphViewModels<SendEvmNonceViewModel>(navGraphId) { vmFactory }
}
