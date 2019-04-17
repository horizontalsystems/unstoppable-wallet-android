package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.core.utils.AddressParser
import io.horizontalsystems.bankwallet.entities.AuthData
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bitcoinkit.BitcoinKit

class BitcoinCashAdapter(
        override val coin: Coin,
        authData: AuthData,
        newWallet: Boolean,
        testMode: Boolean,
        private val feeRateProvider: IFeeRateProvider)
    : BitcoinBaseAdapter(coin, AddressParser("bitcoincash", false)) {

    override val networkType = if (testMode) BitcoinKit.NetworkType.TestNetBitCash else BitcoinKit.NetworkType.MainNetBitCash

    override val bitcoinKit = BitcoinKit(authData.seed, networkType, newWallet = newWallet, walletId = authData.walletId)

    override fun feeRate(feePriority: FeeRatePriority): Int {
        return feeRateProvider.bitcoinCashFeeRate(feePriority).toInt()
    }

}
