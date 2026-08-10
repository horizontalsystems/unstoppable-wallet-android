package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.walletkit.modules.evmfee.GasPriceInfo
import io.horizontalsystems.ethereumkit.models.Address

data class SendTransactionSettingsEvm(
    val gasPriceInfo: GasPriceInfo?,
    val receiveAddress: Address,
) : SendTransactionSettings()
