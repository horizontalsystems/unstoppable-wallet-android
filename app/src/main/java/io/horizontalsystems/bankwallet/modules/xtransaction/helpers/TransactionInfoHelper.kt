package io.horizontalsystems.bankwallet.modules.xtransaction.helpers

import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.contacts.ContactsRepository
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.marketkit.models.BlockchainType
import java.math.BigDecimal
import javax.inject.Inject

class TransactionInfoHelper @Inject constructor(
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val contactsRepository: ContactsRepository,
) {

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
