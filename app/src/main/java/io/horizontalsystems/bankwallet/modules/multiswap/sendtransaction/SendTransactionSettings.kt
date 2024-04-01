package io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction

import io.horizontalsystems.bankwallet.modules.evmfee.GasPriceInfo
import io.horizontalsystems.ethereumkit.models.Address

sealed class SendTransactionSettings {
    data class Evm(val gasPriceInfo: GasPriceInfo?, val receiveAddress: Address) : SendTransactionSettings()
}
