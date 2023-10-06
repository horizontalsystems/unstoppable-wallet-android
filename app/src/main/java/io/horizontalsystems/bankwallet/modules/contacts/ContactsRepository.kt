package io.horizontalsystems.bankwallet.modules.contacts

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.modules.contacts.ContactsModule.ContactValidationException
import io.horizontalsystems.bankwallet.modules.contacts.model.Contact
import io.horizontalsystems.bankwallet.modules.contacts.model.ContactAddress
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.Executors

class ContactsRepository(
    private val marketKit: MarketKitWrapper
) {
    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(singleDispatcher)
    private val gson by lazy { Gson() }

    private var contactsMap: MutableMap<String, Contact> = mutableMapOf()

    private val logger = AppLogger("contacts")

    private val file: File
        get() = File(App.instance.filesDir, "UW_Contacts.json")

    val contacts: List<Contact>
        get() = contactsMap.map { it.value }.sortedBy { it.name }.toList()

    val asJsonString: String
        get() = gson.toJson(contacts.map { ContactJson(it) })

    private val _contactsFlow = MutableStateFlow(contacts)
    val contactsFlow: StateFlow<List<Contact>> = _contactsFlow

    fun getContactsFiltered(
        blockchainType: BlockchainType? = null,
        nameQuery: String? = null,
        addressQuery: String? = null,
    ): List<Contact> {
        val criteria = mutableListOf<(Contact) -> Boolean>()

        nameQuery?.let {
            criteria.add {
                it.name.contains(nameQuery, true)
            }
        }

        if (addressQuery != null) {
            criteria.add {
                it.addresses.any { contactAddress ->
                    (blockchainType == null || blockchainType == contactAddress.blockchain.type)
                            && contactAddress.address.equals(addressQuery, true)
                }
            }
        } else if (blockchainType != null) {
            criteria.add {
                it.addresses.any { it.blockchain.type == blockchainType }
            }
        }

        return contacts.filter { contact ->
            criteria.all {
                it.invoke(contact)
            }
        }
    }

    fun initialize() {
        coroutineScope.launch {
            readFromFile()
        }
    }

    @Throws
    fun validateContactName(contactUid: String, name: String) {
        if (contacts.any { it.uid != contactUid && it.name == name }) {
            throw ContactValidationException.DuplicateContactName
        }
    }

    @Throws
    fun validateAddress(contactUid: String?, address: ContactAddress) {
        val contactWithSameAddress = contacts.find { it.uid != contactUid && it.addresses.contains(address) }

        if (contactWithSameAddress != null) {
            throw ContactValidationException.DuplicateAddress(contactWithSameAddress)
        }
    }

    fun save(contact: Contact) {
        contactsMap[contact.uid] = contact
        _contactsFlow.update { contacts }

        coroutineScope.launch {
            writeToFile()
        }
    }

    fun restore(contacts: List<Contact>) {
        contactsMap = contacts.associateBy { it.uid }.toMutableMap()
        _contactsFlow.update { contacts }

        coroutineScope.launch {
            writeToFile()
        }
    }

    fun get(id: String): Contact? {
        return contactsMap[id]
    }

    fun delete(id: String) {
        contactsMap.remove(id)
        _contactsFlow.update { contacts }

        coroutineScope.launch {
            writeToFile()
        }
    }

    private fun parseAndRestore(json: String) {
        val contacts = parseFromJson(json)
        contactsMap = contacts.associateBy { it.uid }.toMutableMap()

        _contactsFlow.update { contacts }
    }

    fun restore(json: String) {
        parseAndRestore(json)

        coroutineScope.launch {
            writeToFile()
        }
    }

    private fun readFromFile() {
        try {
            val json = FileInputStream(file).use { fis ->
                fis.bufferedReader().use { br ->
                    br.readText()
                }
            }
            parseAndRestore(json)
        } catch (e: Throwable) {
            if (file.exists()) {
                logger.warning("readFromFile() error", e)
            }
        }
    }

    fun parseFromJson(json: String): List<Contact> {
        val listType = object : TypeToken<List<ContactJson>>() {}.type
        val contactsJson: List<ContactJson> = gson.fromJson(json, listType)

        return contactsJson.map { contactJson ->
            Contact(
                uid = contactJson.uid,
                name = contactJson.name,
                addresses = contactJson.addresses.mapNotNull { addressJson ->
                    marketKit.blockchain(addressJson.blockchain_uid)?.let { ContactAddress(it, addressJson.address) }
                }
            )
        }
    }

    private fun writeToFile() {
        try {
            FileOutputStream(file).use { fos ->
                fos.bufferedWriter().use { bw ->
                    bw.write(asJsonString)
                }
            }
        } catch (e: Throwable) {
            logger.warning("writeToFile() error", e)
        }
    }

    data class ContactJson(
        val uid: String,
        val name: String,
        val addresses: List<AddressJson>
    ) {
        constructor(contact: Contact) : this(
            contact.uid,
            contact.name,
            contact.addresses.map { AddressJson(it.blockchain.uid, it.address) }
        )

        data class AddressJson(
            val blockchain_uid: String,
            val address: String
        )
    }
}
