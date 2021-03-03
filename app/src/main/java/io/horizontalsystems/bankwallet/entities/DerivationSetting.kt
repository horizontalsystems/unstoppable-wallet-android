package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.entities.AccountType.Derivation
import io.horizontalsystems.coinkit.models.CoinType

class DerivationSetting(val coinType: CoinType,
                        var derivation: Derivation)
