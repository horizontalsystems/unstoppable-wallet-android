package io.horizontalsystems.bankwallet.entities

import java.math.BigDecimal

data class CurrencyValue(val currency: Currency, val value: BigDecimal)