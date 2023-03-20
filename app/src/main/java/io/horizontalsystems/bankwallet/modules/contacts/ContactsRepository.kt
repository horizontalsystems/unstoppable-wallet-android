package io.horizontalsystems.bankwallet.modules.contacts

import com.google.gson.Gson
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
    private var deletedContacts: MutableList<DeletedContact> = mutableListOf()

    private val logger = AppLogger("contacts")

    private val file: File
        get() = File(App.instance.filesDir, "UW_Contacts.json")

    val contacts: List<Contact>
        get() = contactsMap.map { it.value }.sortedBy { it.name }.toList()

    private val asJsonString: String
        get() = gson.toJson(ContactsJson(contacts.map { ContactJson(it) }, deletedContacts))

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

    fun get(id: String): Contact? {
        return contactsMap[id]
    }

    fun delete(id: String) {
        contactsMap.remove(id)
        _contactsFlow.update { contacts }

        deletedContacts.add(DeletedContact(id, System.currentTimeMillis() / 1000))

        coroutineScope.launch {
            writeToFile()
        }
    }

    fun export(): String {
        return asJsonString
    }

    fun import(json: String) {
        val (contacts, deleted) = parseFromJson(json)

        contacts.forEach { importingContact ->
            val contact = contactsMap[importingContact.uid]
            val deletedContact = deletedContacts.find { it.uid == importingContact.uid }

            when {
                contact == null && deletedContact == null -> {
                    // no contact and no deleted contact
                    contactsMap[importingContact.uid] = importingContact
                }
                contact != null && contact.modifiedTimestamp <= importingContact.modifiedTimestamp -> {
                    // importing contact is newer than local contact
                    contactsMap[importingContact.uid] = importingContact
                }
                deletedContact != null && deletedContact.deleted_at <= importingContact.modifiedTimestamp -> {
                    // importing contact is newer than local deleted contact
                    contactsMap[importingContact.uid] = importingContact
                    deletedContacts.remove(deletedContact)
                }
            }
        }

        deleted.forEach { importingDeletedContact ->
            val contact = contactsMap[importingDeletedContact.uid]
            val deletedContact = deletedContacts.find { it.uid == importingDeletedContact.uid }

            when {
                contact == null && deletedContact == null -> {
                    // importing deleted contact does not exist locally
                    deletedContacts.add(importingDeletedContact)
                }
                contact != null && contact.modifiedTimestamp <= importingDeletedContact.deleted_at -> {
                    // importing deleted contact is newer than local contact
                    contactsMap.remove(importingDeletedContact.uid)
                    deletedContacts.add(importingDeletedContact)
                }
                deletedContact != null && deletedContact.deleted_at < importingDeletedContact.deleted_at -> {
                    // importing deleted contact is newer than local deleted contact
                    deletedContacts.remove(deletedContact)
                    deletedContacts.add(importingDeletedContact)
                }
            }
        }

        _contactsFlow.update { contacts }

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
            val (contacts, deleted) = parseFromJson(json)

            contactsMap = contacts.associateBy { it.uid }.toMutableMap()
            deletedContacts = deleted.toMutableList()
        } catch (e: Throwable) {
            if (file.exists()) {
                logger.warning("readFromFile() error", e)
            }
        }
    }

    private fun parseFromJson(json: String): Pair<List<Contact>, List<DeletedContact>> {
        val contactsJson = gson.fromJson(json, ContactsJson::class.java)
        val contacts = contactsJson.contacts.map { contactJson ->
            Contact(
                uid = contactJson.uid,
                name = contactJson.name,
                addresses = contactJson.addresses.mapNotNull { addressJson ->
                    marketKit.blockchain(addressJson.blockchainUid)?.let { ContactAddress(it, addressJson.address) }
                },
                modifiedTimestamp = contactJson.modified_at
            )
        }
        return Pair(contacts, contactsJson.deleted)
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

    data class ContactsJson(
        val contacts: List<ContactJson>,
        val deleted: List<DeletedContact>
    )

    data class DeletedContact(
        val uid: String,
        val deleted_at: Long
    )

    data class ContactJson(
        val uid: String,
        val name: String,
        val addresses: List<AddressJson>,
        val modified_at: Long
    ) {
        constructor(contact: Contact) : this(
            contact.uid,
            contact.name,
            contact.addresses.map { AddressJson(it.blockchain.uid, it.address) },
            contact.modifiedTimestamp,
        )

        data class AddressJson(
            val blockchainUid: String,
            val address: String
        )
    }
}
