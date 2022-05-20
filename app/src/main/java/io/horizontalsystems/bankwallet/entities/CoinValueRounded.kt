package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.managers.BigDecimalRounded
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.PlatformCoin

data class CoinValueRounded(val platformCoin: PlatformCoin, val value: BigDecimalRounded)
data class CurrencyValueRounded(val currency: Currency, val value: BigDecimalRounded)
