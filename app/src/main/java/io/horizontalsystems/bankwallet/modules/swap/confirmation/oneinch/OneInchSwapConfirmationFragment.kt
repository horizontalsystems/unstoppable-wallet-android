package io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch

import androidx.core.os.bundleOf
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.settings.SendEvmNonceViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.OneInchSwapParameters
import io.horizontalsystems.bankwallet.modules.swap.confirmation.BaseSwapConfirmationFragment
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
        ) = bundleOf(
            blockchainTypeKey to blockchainType,
            oneInchSwapParametersKey to oneInchSwapParameters
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
