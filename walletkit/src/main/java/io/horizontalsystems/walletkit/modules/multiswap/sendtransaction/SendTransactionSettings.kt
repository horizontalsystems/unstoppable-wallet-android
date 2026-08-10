package io.horizontalsystems.walletkit.modules.multiswap.sendtransaction

abstract class SendTransactionSettings {
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
