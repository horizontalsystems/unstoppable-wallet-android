package com.quantum.wallet.bankwallet.modules.multiswap.sendtransaction

import com.quantum.wallet.bankwallet.modules.evmfee.GasPriceInfo
import io.horizontalsystems.ethereumkit.models.Address

sealed class SendTransactionSettings {
    data class Evm(val gasPriceInfo: GasPriceInfo?, val receiveAddress: Address) : SendTransactionSettings()
    class Btc : SendTransactionSettings()
    class Tron : SendTransactionSettings()
    class Stellar : SendTransactionSettings()
    class Ton : SendTransactionSettings()
    class Zcash : SendTransactionSettings()
    class Monero : SendTransactionSettings()
    class Solana : SendTransactionSettings()
}
