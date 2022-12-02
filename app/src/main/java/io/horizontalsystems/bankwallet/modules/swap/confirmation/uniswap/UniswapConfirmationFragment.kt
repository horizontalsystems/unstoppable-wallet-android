package io.horizontalsystems.bankwallet.modules.swap.confirmation.uniswap

import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.modules.evmfee.EvmFeeCellViewModel
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmData
import io.horizontalsystems.bankwallet.modules.send.evm.SendEvmModule
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.swap.confirmation.BaseSwapConfirmationFragment
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData

class UniswapConfirmationFragment(
    override val navGraphId: Int = R.id.uniswapConfirmationFragment
) : BaseSwapConfirmationFragment() {

    override val logger = AppLogger("swap_uniswap")

    private val transactionData: TransactionData
        get() {
            val transactionDataParcelable = arguments?.getParcelable<SendEvmModule.TransactionDataParcelable>(SendEvmModule.transactionDataKey)!!
            return TransactionData(
                Address(transactionDataParcelable.toAddress),
                transactionDataParcelable.value,
                transactionDataParcelable.input
            )
        }

    private val additionalInfo: SendEvmData.AdditionalInfo?
        get() = arguments?.getParcelable(SendEvmModule.additionalInfoKey)

    private val vmFactory by lazy {
        UniswapConfirmationModule.Factory(
            dex.blockchainType,
            SendEvmData(transactionData, additionalInfo)
        )
    }
    override val sendEvmTransactionViewModel by viewModels<SendEvmTransactionViewModel> { vmFactory }
    override val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(navGraphId) { vmFactory }

}
