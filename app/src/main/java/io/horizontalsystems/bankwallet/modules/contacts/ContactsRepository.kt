package io.horizontalsystems.bankwallet.modules.contacts

import android.os.Environment
import com.google.gson.Gson
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
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

    val contacts: List<Contact>
        get() = contactsMap.map { it.value }.toList()

    private val _contactsFlow = MutableStateFlow(contacts)
    val contactsFlow: StateFlow<List<Contact>> = _contactsFlow

    fun getContactsFiltered(
        blockchainType: BlockchainType,
        nameQuery: String? = null,
        addressQuery: String? = null,
    ): List<Contact> {
        val criteria = mutableListOf<(Contact) -> Boolean>()

        nameQuery?.let {
            criteria.add {
                it.name.contains(nameQuery, true)
            }
        }

        criteria.add {
            it.addresses.isNotEmpty()
        }

        if (addressQuery != null) {
            criteria.add {
                it.addresses.any {
                    it.blockchain.type == blockchainType && it.address.equals(addressQuery, true)
                }
            }
        } else {
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

    private val file: File
        get() = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "UW_Contacts.json"
        )

    private fun readFromFile() {
        try {
            val json = FileInputStream(file).use { fis ->
                fis.bufferedReader().use { br ->
                    br.readText()
                }
            }
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

            contactsMap = contacts.associateBy { it.uid }.toMutableMap()
            deletedContacts = contactsJson.deleted.toMutableList()
        } catch (e: Throwable) {
            logger.warning("readFromFile() error", e)
        }
    }

    private fun writeToFile() {
        val json = gson.toJson(ContactsJson(contacts.map { ContactJson(it) }, deletedContacts))

        FileOutputStream(file).use { fos ->
            fos.bufferedWriter().use { bw ->
                bw.write(json)
            }
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
