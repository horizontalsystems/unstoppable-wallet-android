package cash.p.terminal.modules.swap.confirmation.oneinch

import android.os.Parcelable
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.getInputX
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.swap.SwapMainModule.OneInchSwapParameters
import cash.p.terminal.modules.swap.confirmation.BaseSwapConfirmationFragment
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.parcelize.Parcelize

class OneInchSwapConfirmationFragment(
    override val navGraphId: Int = R.id.oneInchConfirmationFragment
) : BaseSwapConfirmationFragment() {

    private val input by lazy {
        requireArguments().getInputX<Input>()!!
    }

    override val logger = AppLogger("swap_1inch")

    private val vmFactory by lazy {
        OneInchConfirmationModule.Factory(input.blockchainType, input.oneInchSwapParameters)
    }

    override val swapEntryPointDestId: Int
        get() = input.swapEntryPointDestId
    override val sendEvmTransactionViewModel by navGraphViewModels<SendEvmTransactionViewModel>(navGraphId) { vmFactory }
    override val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(navGraphId) { vmFactory }
    override val nonceViewModel by navGraphViewModels<SendEvmNonceViewModel>(navGraphId) { vmFactory }

    @Parcelize
    data class Input(
        val blockchainType: BlockchainType,
        val oneInchSwapParameters: OneInchSwapParameters,
        val swapEntryPointDestId: Int,
    ) : Parcelable
}
