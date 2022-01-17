package io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch

import androidx.fragment.app.viewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.ethereum.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.confirmation.BaseSwapConfirmationFragment
import io.horizontalsystems.core.findNavController

class OneInchSwapConfirmationFragment : BaseSwapConfirmationFragment() {
    override val logger = AppLogger("swap_1inch")

    private val vmFactory by lazy { OneInchConfirmationModule.Factory(dex.blockchain, requireArguments()) }
    override val sendViewModel by viewModels<SendEvmTransactionViewModel> { vmFactory }
    override val feeViewModel by viewModels<EvmFeeCellViewModel> { vmFactory }

    override fun navigateToFeeInfo() {
        findNavController().slideFromRight(R.id.oneInchConfirmationFragment_to_feeSpeedInfo)
    }

}
