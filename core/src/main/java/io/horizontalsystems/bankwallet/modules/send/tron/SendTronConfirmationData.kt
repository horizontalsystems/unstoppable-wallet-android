package io.horizontalsystems.bankwallet.modules.send.tron

import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal

data class SendTronConfirmationData(
    val amount: BigDecimal,
    val address: Address,
    val fee: BigDecimal?,
    val activationFee: BigDecimal?,
    val resourcesConsumed: String?,
    val contact: Contact?,
    val token: Token,
    val feeCoin: Coin,
    val isInactiveAddress: Boolean,
    val memo: String? = null,
)
