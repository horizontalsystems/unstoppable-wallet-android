package cash.p.terminal.modules.swap.confirmation.uniswap

import android.os.Parcelable
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.getInputX
import cash.p.terminal.modules.evmfee.EvmFeeCellViewModel
import cash.p.terminal.modules.send.evm.SendEvmData
import cash.p.terminal.modules.send.evm.SendEvmModule
import cash.p.terminal.modules.send.evm.settings.SendEvmNonceViewModel
import cash.p.terminal.modules.sendevmtransaction.SendEvmTransactionViewModel
import cash.p.terminal.modules.swap.SwapMainModule
import cash.p.terminal.modules.swap.confirmation.BaseSwapConfirmationFragment
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.TransactionData
import kotlinx.parcelize.Parcelize

class UniswapConfirmationFragment(
    override val navGraphId: Int = R.id.uniswapConfirmationFragment
) : BaseSwapConfirmationFragment() {

    private val input by lazy {
        requireArguments().getInputX<Input>()!!
    }

    override val logger = AppLogger("swap_uniswap")

    private val vmFactory by lazy {
        UniswapConfirmationModule.Factory(
            input.dex,
            input.transactionData,
            input.additionalInfo
        )
    }

    override val swapEntryPointDestId: Int
        get() = input.swapEntryPointDestId
    override val sendEvmTransactionViewModel by navGraphViewModels<SendEvmTransactionViewModel>(navGraphId) { vmFactory }
    override val feeViewModel by navGraphViewModels<EvmFeeCellViewModel>(navGraphId) { vmFactory }
    override val nonceViewModel by navGraphViewModels<SendEvmNonceViewModel>(navGraphId) { vmFactory }

    @Parcelize
    data class Input(
        val dex: SwapMainModule.Dex,
        val transactionDataParcelable: SendEvmModule.TransactionDataParcelable,
        val additionalInfo: SendEvmData.AdditionalInfo?,
        val swapEntryPointDestId: Int
    ) : Parcelable {
        val transactionData: TransactionData
            get() = TransactionData(
                Address(transactionDataParcelable.toAddress),
                transactionDataParcelable.value,
                transactionDataParcelable.input
            )
    }
}
