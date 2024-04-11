package io.horizontalsystems.bankwallet.modules.send

import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.Coin
import java.math.BigDecimal

data class SendConfirmationData(
    val amount: BigDecimal,
    val fee: BigDecimal,
    val address: Address,
    val contact: Contact?,
    val coin: Coin,
    val feeCoin: Coin,
    val lockTimeInterval: LockTimeInterval? = null,
    val memo: String?,
    val rbfEnabled: Boolean? = null
)
