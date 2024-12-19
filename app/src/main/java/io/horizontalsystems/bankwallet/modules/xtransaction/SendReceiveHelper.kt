package io.horizontalsystems.bankwallet.modules.xtransaction

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

class SendReceiveHelper {
    private val marketKit = App.marketKit
    private val currencyManager = App.currencyManager
    private val contactsRepository = App.contactsRepository

    fun getXRate(coinUid: String): BigDecimal? {
        return marketKit.coinPrice(coinUid, currencyManager.baseCurrency.code)?.value
    }

    fun getCurrencySymbol(): String {
        return currencyManager.baseCurrency.symbol
    }

    fun getContact(address: String?, blockchainType: BlockchainType): Contact? {
        return contactsRepository
            .getContactsFiltered(blockchainType, addressQuery = address)
            .firstOrNull()
    }
}
