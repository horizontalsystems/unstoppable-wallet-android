package io.horizontalsystems.walletkit.modules.multiswap

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.chain.SendChainSettings
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.isNative
import java.math.BigDecimal

/**
 * The one check a deposit gets before its order exists when no send service has estimated it:
 * the amount against what the wallet can spend. A deposit refused later, after the commit, leaves
 * an order that can never be funded, so this errs on the side of refusing.
 */
object UnpricedDeposit {

    /**
     * What a deposit of [token] can draw on: the chain's fee- and reserve-aware maximum where it
     * has one (also the outputs chosen on the send screen, through [settings]), otherwise the
     * wallet's spendable balance. Null when the wallet does not know its balance yet.
     */
    fun availableBalance(token: Token, settings: SendChainSettings?): BigDecimal? =
        ChainRegistry[token.blockchainType]?.sendAvailableBalance(token, settings)
            ?: App.adapterManager.getAdapterForToken<IBalanceAdapter>(token)?.balanceData?.available

    /**
     * The insufficient-balance caution when [amount] cannot be deposited out of [available], or
     * null. A coin that pays its own network fee (native, including the derived and address-typed
     * Bitcoin-family forms) pays it out of the same balance, and the fee is exactly what has not
     * been estimated, so the whole balance is never enough for it.
     */
    fun caution(token: Token, amount: BigDecimal, available: BigDecimal?): CautionViewItem? {
        if (available == null) return null

        val insufficient = if (token.type.isNative) amount >= available else amount > available
        return if (insufficient) CautionViewItem.insufficientBalance(token.coin.code) else null
    }
}
