package cash.p.terminal.modules.swap.confirmation.oneinch

import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.swap.confirmation.BaseSwapConfirmationFragment

class OneInchSwapConfirmationFragment(
    override val navGraphId: Int = R.id.oneInchConfirmationFragment
) : BaseSwapConfirmationFragment() {

    override val logger = AppLogger("swap_1inch")

    private val vmFactory by lazy {
        OneInchConfirmationModule.Factory(dex.blockchainType, requireArguments())
    }
    override val sendEvmTransactionViewModel by navGraphViewModels<SendEvmTransactionViewModel>(navGraphId) { vmFactory }
    override val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(navGraphId) { vmFactory }
    override val nonceViewModel by navGraphViewModels<SendEvmNonceViewModel>(navGraphId) { vmFactory }
}
