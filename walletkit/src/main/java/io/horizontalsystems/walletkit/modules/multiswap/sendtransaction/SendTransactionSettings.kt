package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

import io.horizontalsystems.walletkit.modules.evmfee.GasPriceInfo
import io.horizontalsystems.ethereumkit.models.Address

sealed class SendTransactionSettings {
    data class Evm(val gasPriceInfo: GasPriceInfo?, val receiveAddress: Address) : SendTransactionSettings()
    class Btc : SendTransactionSettings()
    class Tron : SendTransactionSettings()
    class Stellar : SendTransactionSettings()
    class Ton : SendTransactionSettings()
    class Zcash : SendTransactionSettings()
    class Monero : SendTransactionSettings()
    class Thorchain : SendTransactionSettings()
    class Zano : SendTransactionSettings()
    class Solana : SendTransactionSettings()
}
