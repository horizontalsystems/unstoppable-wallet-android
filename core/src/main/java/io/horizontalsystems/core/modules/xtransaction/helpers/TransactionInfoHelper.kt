package io.horizontalsystems.core.modules.xtransaction.helpers

import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.modules.contacts.model.Contact
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal

class TransactionInfoHelper {
    private val marketKit = App.marketKit
    private val currencyManager = App.currencyManager
    private val contactsRepository = App.contactsRepository

    fun getXRate(coinUid: String): BigDecimal? {
        return marketKit.coinPrice(coinUid, currencyManager.baseCurrency.code)?.value
    }

    fun getCurrency(): Currency {
        return currencyManager.baseCurrency
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
