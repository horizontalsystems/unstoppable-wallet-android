package io.horizontalsystems.core.modules.send

import io.horizontalsystems.core.entities.Address
import io.horizontalsystems.core.modules.contacts.model.Contact
import io.horizontalsystems.hodler.LockTimeInterval
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

data class SendConfirmationData(
    val amount: BigDecimal,
    val fee: BigDecimal?,
    val address: Address?,
    val contact: Contact?,
    val token: Token,
    val feeCoin: Coin,
    val lockTimeInterval: LockTimeInterval? = null,
    val memo: String?,
    val rbfEnabled: Boolean? = null,
    val error: Throwable? = null
)
