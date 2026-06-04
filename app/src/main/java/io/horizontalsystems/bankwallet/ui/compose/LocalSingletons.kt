package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.runtime.staticCompositionLocalOf
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper

/**
 * App-wide singletons exposed to composables so screens don't read `App.*` directly.
 * Provided once at the Compose root (see MainActivity). Accessing `.current` without a
 * provider is a programming error and throws.
 */
val LocalNumberFormatter = staticCompositionLocalOf<IAppNumberFormatter> {
    error("LocalNumberFormatter not provided")
}

val LocalCurrencyManager = staticCompositionLocalOf<CurrencyManager> {
    error("LocalCurrencyManager not provided")
}

val LocalMarketKit = staticCompositionLocalOf<MarketKitWrapper> {
    error("LocalMarketKit not provided")
}
