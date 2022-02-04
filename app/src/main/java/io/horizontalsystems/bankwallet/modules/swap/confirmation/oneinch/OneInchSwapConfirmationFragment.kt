package io.horizontalsystems.bankwallet.modules.swap.confirmation.oneinch

import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.confirmation.BaseSwapConfirmationFragment

class OneInchSwapConfirmationFragment(
    override val fragmentId: Int = R.id.oneInchConfirmationFragment
) : BaseSwapConfirmationFragment() {

    override val logger = AppLogger("swap_1inch")

    private val vmFactory by lazy { OneInchConfirmationModule.Factory(dex.blockchain, requireArguments()) }
    override val sendEvmTransactionViewModel by viewModels<SendEvmTransactionViewModel> { vmFactory }
    override val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(fragmentId) { vmFactory }
}
