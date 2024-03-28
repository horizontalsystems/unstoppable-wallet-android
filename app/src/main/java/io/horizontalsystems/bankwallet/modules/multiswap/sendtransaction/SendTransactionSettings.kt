package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.modules.evmfee.GasPriceInfo

sealed class SendTransactionSettings {
    data class Evm(val gasPriceInfo: GasPriceInfo?) : SendTransactionSettings()
}
