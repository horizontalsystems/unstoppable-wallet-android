package cash.p.terminal.modules.swapxxx.sendtransaction

import cash.p.terminal.modules.evmfee.GasPriceInfo

sealed class SendTransactionSettings {
    data class Evm(val gasPriceInfo: GasPriceInfo?) : SendTransactionSettings()
}
