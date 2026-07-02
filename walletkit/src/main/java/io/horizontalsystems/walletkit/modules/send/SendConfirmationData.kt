package io.horizontalsystems.walletkit.modules.send

import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.contacts.model.Contact
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
