package io.horizontalsystems.bankwallet.modules.swapxxx.sendtransaction

import io.horizontalsystems.bankwallet.modules.evmfee.GasPriceInfo

sealed class SendTransactionSettings {
    data class Evm(val gasPriceInfo: GasPriceInfo?) : SendTransactionSettings()
}
