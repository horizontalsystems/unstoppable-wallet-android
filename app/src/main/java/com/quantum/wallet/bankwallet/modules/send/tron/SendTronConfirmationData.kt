package com.quantum.wallet.bankwallet.modules.send.tron

import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.modules.contacts.model.Contact
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
