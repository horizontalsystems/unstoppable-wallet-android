package cash.p.terminal.modules.multiswap.sendtransaction

import cash.p.terminal.modules.evmfee.GasPriceInfo

sealed class SendTransactionSettings {
    data class Evm(val gasPriceInfo: GasPriceInfo?) : SendTransactionSettings()
}
