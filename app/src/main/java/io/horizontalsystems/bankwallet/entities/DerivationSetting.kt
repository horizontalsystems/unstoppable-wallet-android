package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.entities.AccountType.Derivation

class DerivationSetting(val coinType: CoinType,
                        var derivation: Derivation)
