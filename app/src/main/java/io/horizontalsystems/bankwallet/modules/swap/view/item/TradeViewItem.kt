package io.horizontalsystems.bankwallet.modules.swap.view.item

import io.horizontalsystems.bankwallet.modules.swap.model.PriceImpact

data class TradeViewItem(
        val price: String? = null,
        val priceImpact: String? = null,
        val priceImpactLevel: PriceImpact.Level? = null,
        val minMaxTitle: String? = null,
        val minMaxAmount: String? = null
)
