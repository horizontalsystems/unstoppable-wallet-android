package cash.p.terminal.modules.multiswap.sendtransaction

import cash.p.terminal.modules.evmfee.GasPriceInfo
import io.horizontalsystems.ethereumkit.models.Address

sealed class SendTransactionSettings {
    data object Common : SendTransactionSettings()
    data class Evm(val gasPriceInfo: GasPriceInfo?, val receiveAddress: Address) : SendTransactionSettings()
}
