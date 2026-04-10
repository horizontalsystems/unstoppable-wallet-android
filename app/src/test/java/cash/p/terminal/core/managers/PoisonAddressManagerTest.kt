package cash.p.terminal.core.managers

import cash.p.terminal.core.storage.PoisonAddressDao
import cash.p.terminal.entities.PoisonAddress
import cash.p.terminal.entities.PoisonAddressType
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.TransferEvent
import cash.p.terminal.entities.transactionrecords.tron.TronTransactionRecord
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.transactions.poison_status.PoisonStatus
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@Suppress("LargeClass")
class PoisonAddressManagerTest {

    private val dao = mockk<PoisonAddressDao>(relaxed = true)
    private val contactsRepository = mockk<ContactsRepository>()
    private val marketKit = mockk<MarketKitWrapper>()
    private val blockchainType = BlockchainType.Ethereum
    private val blockchainUid = blockchainType.uid
    private val accountId = "test-account-id"

    private lateinit var manager: PoisonAddressManager

    @Before
    fun setup() {
        every { contactsRepository.getContactsFiltered(any(), any(), any()) } returns emptyList()
        every { dao.get(any(), any(), any()) } returns null
        every { dao.getAllByType(any(), any(), any()) } returns emptyList()
        every { marketKit.token(any<TokenQuery>()) } returns null

        manager = PoisonAddressManager(dao, contactsRepository, marketKit)
    }

    // --- determinePoisonStatus ---

    @Test
    fun determinePoisonStatus_nullAddress_returnsBlockchain() {
        val result = manager.determinePoisonStatus(
            relevantAddress = null,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_addressInContacts_returnsAddressBook() {
        val address = "0xabc123def456"
        every {
            contactsRepository.getContactsFiltered(
                blockchainType = blockchainType,
                addressQuery = address,
            )
        } returns listOf(mockk<Contact>())

        val result = manager.determinePoisonStatus(
            relevantAddress = address,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.ADDRESS_BOOK, result)
    }

    @Test
    fun determinePoisonStatus_outgoingCreatedByWalletButInContacts_returnsAddressBook() {
        val address = "0xabc123def456"
        every {
            contactsRepository.getContactsFiltered(
                blockchainType = blockchainType,
                addressQuery = address,
            )
        } returns listOf(mockk<Contact>())

        val result = manager.determinePoisonStatus(
            relevantAddress = address,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = true,
            isCreatedByWallet = true,
        )
        assertEquals(PoisonStatus.ADDRESS_BOOK, result)
    }

    @Test
    fun determinePoisonStatus_outgoingCreatedByWallet_returnsCreated() {
        val result = manager.determinePoisonStatus(
            relevantAddress = "0xabc123def456",
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = true,
            isCreatedByWallet = true,
        )
        assertEquals(PoisonStatus.CREATED, result)
    }

    @Test
    fun determinePoisonStatus_knownAddress_returnsBlockchain() {
        val address = "0xabc123def456"
        every { dao.get(address, blockchainUid, accountId) } returns
            PoisonAddress(address, blockchainUid, accountId, PoisonAddressType.KNOWN)

        val result = manager.determinePoisonStatus(
            relevantAddress = address,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_scamAddress_returnsSuspicious() {
        val address = "0xabc123def456"
        every { dao.get(address, blockchainUid, accountId) } returns
            PoisonAddress(address, blockchainUid, accountId, PoisonAddressType.SCAM)

        val result = manager.determinePoisonStatus(
            relevantAddress = address,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun determinePoisonStatus_similarToKnown_returnsSuspicious() {
        val knownAddress = "0xaaaa_middle_bbb"
        val similarAddress = "0xa_different_bbb"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid, accountId) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, accountId, PoisonAddressType.KNOWN))

        val result = manager.determinePoisonStatus(
            relevantAddress = similarAddress,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun determinePoisonStatus_notSimilarToKnown_returnsBlockchain() {
        val knownAddress = "0xaaaa_middle_bbb"
        val differentAddress = "0xz_different_zzz"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid, accountId) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, accountId, PoisonAddressType.KNOWN))

        val result = manager.determinePoisonStatus(
            relevantAddress = differentAddress,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    // --- Case insensitivity ---

    @Test
    fun determinePoisonStatus_sameAddressDifferentCase_returnsCorrectStatus() {
        val lowerAddress = "0xabcdef123456"
        val mixedCaseAddress = "0xAbCdEf123456"
        every { dao.get(lowerAddress, blockchainUid, accountId) } returns
            PoisonAddress(lowerAddress, blockchainUid, accountId, PoisonAddressType.SCAM)

        val result = manager.determinePoisonStatus(
            relevantAddress = mixedCaseAddress,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun saveKnownAddress_storesLowercased() {
        val mixedCase = "0xAbCdEf123456"
        manager.saveKnownAddress(mixedCase, blockchainType, accountId)

        verify {
            dao.insert(
                PoisonAddress("0xabcdef123456", blockchainUid, accountId, PoisonAddressType.KNOWN)
            )
        }
    }

    // --- Account scoping ---

    @Test
    fun determinePoisonStatus_knownForDifferentAccount_notTreatedAsKnown() {
        val address = "0xabc123def456"
        val otherAccountId = "other-account-id"
        every { dao.get(address, blockchainUid, accountId) } returns
            PoisonAddress(address, blockchainUid, accountId, PoisonAddressType.KNOWN)
        every { dao.get(address, blockchainUid, otherAccountId) } returns null

        val result = manager.determinePoisonStatus(
            relevantAddress = address,
            blockchainType = blockchainType,
            accountId = otherAccountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_similarToKnownOfDifferentAccount_notFlagged() {
        val knownAddress = "abc_known_xyz"
        val similarAddress = "abc_poison_xyz"
        val otherAccountId = "other-account-id"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid, accountId) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, accountId, PoisonAddressType.KNOWN))
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid, otherAccountId) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = similarAddress,
            blockchainType = blockchainType,
            accountId = otherAccountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun isAddressSuspicious_knownForDifferentAccount_notTreatedAsKnown() {
        val address = "0xabc123def456"
        val otherAccountId = "other-account-id"
        every { dao.get(address, blockchainUid, accountId) } returns
            PoisonAddress(address, blockchainUid, accountId, PoisonAddressType.KNOWN)
        every { dao.get(address, blockchainUid, otherAccountId) } returns null

        assertFalse(manager.isAddressSuspicious(address, blockchainType, otherAccountId))
    }

    @Test
    fun saveKnownAddress_scopedToAccount() {
        val address = "0xabc123def456"
        manager.saveKnownAddress(address, blockchainType, accountId)

        verify {
            dao.insert(PoisonAddress(address, blockchainUid, accountId, PoisonAddressType.KNOWN))
        }
    }

    // --- Similarity edge cases ---

    @Test
    fun determinePoisonStatus_similarFirst3Last3Match_returnsSuspicious() {
        val knownAddress = "abc_known_xyz"
        val similarAddress = "abc_poison_xyz"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid, accountId) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, accountId, PoisonAddressType.KNOWN))

        val result = manager.determinePoisonStatus(
            relevantAddress = similarAddress,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun determinePoisonStatus_onlyFirst3Match_returnsBlockchain() {
        val knownAddress = "abc_known_xyz"
        val partialMatch = "abc_poison_qqq"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid, accountId) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, accountId, PoisonAddressType.KNOWN))

        val result = manager.determinePoisonStatus(
            relevantAddress = partialMatch,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_sameAddressAsKnown_returnsBlockchain() {
        val address = "abc_same_xyz"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid, accountId) } returns
            listOf(PoisonAddress(address, blockchainUid, accountId, PoisonAddressType.KNOWN))

        val result = manager.determinePoisonStatus(
            relevantAddress = address,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_shortAddress_returnsBlockchain() {
        val shortAddress = "abcde"  // length 5 < SIMILARITY_CHARS * 2 = 6
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid, accountId) } returns
            listOf(PoisonAddress("abcxe", blockchainUid, accountId, PoisonAddressType.KNOWN))

        val result = manager.determinePoisonStatus(
            relevantAddress = shortAddress,
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    // --- isAddressSuspicious ---

    @Test
    fun isAddressSuspicious_knownAddress_returnsFalse() {
        val address = "0xabc123def456"
        every { dao.get(address, blockchainUid, accountId) } returns
            PoisonAddress(address, blockchainUid, accountId, PoisonAddressType.KNOWN)

        assertFalse(manager.isAddressSuspicious(address, blockchainType, accountId))
    }

    @Test
    fun isAddressSuspicious_scamAddress_returnsTrue() {
        val address = "0xabc123def456"
        every { dao.get(address, blockchainUid, accountId) } returns
            PoisonAddress(address, blockchainUid, accountId, PoisonAddressType.SCAM)

        assertTrue(manager.isAddressSuspicious(address, blockchainType, accountId))
    }

    @Test
    fun isAddressSuspicious_similarToKnown_returnsTrue() {
        val knownAddress = "abc_known_xyz"
        val similarAddress = "abc_poison_xyz"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid, accountId) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, accountId, PoisonAddressType.KNOWN))

        assertTrue(manager.isAddressSuspicious(similarAddress, blockchainType, accountId))
    }

    @Test
    fun isAddressSuspicious_inContacts_returnsFalse() {
        val address = "0xabc123def456"
        every {
            contactsRepository.getContactsFiltered(
                blockchainType = blockchainType,
                addressQuery = address,
            )
        } returns listOf(mockk<Contact>())

        assertFalse(manager.isAddressSuspicious(address, blockchainType, accountId))
    }

    @Test
    fun isAddressSuspicious_nullAddress_returnsFalse() {
        assertFalse(manager.isAddressSuspicious(null, blockchainType, accountId))
    }

    // --- Zero-amount outgoing ---

    @Test
    fun determinePoisonStatus_zeroAmountOutgoing_returnsSuspicious() {
        val result = manager.determinePoisonStatus(
            relevantAddress = "0xabc123def456",
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = true,
            isCreatedByWallet = false,
            amount = BigDecimal.ZERO,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun determinePoisonStatus_not_zeroAmountOutgoing_returnsBlockchain() {
        val result = manager.determinePoisonStatus(
            relevantAddress = "0xabc123def456",
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = true,
            isCreatedByWallet = false,
            amount = BigDecimal("0.00000000000001"),
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_nonZeroAmountOutgoing_notSuspiciousByAmount() {
        val result = manager.determinePoisonStatus(
            relevantAddress = "0xzzz999qqq111",
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = true,
            isCreatedByWallet = false,
            amount = BigDecimal.ONE,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_zeroAmountIncoming_notSuspiciousByAmount() {
        val result = manager.determinePoisonStatus(
            relevantAddress = "0xzzz999qqq111",
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
            amount = BigDecimal.ZERO,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    // --- Fake stablecoin ---

    @Test
    fun determinePoisonStatus_fakeUsdt_returnsSuspicious() {
        val fakeContract = "0xfake_usdt_contract"
        every { marketKit.token(TokenQuery(blockchainType, TokenType.Eip20(fakeContract))) } returns null

        val result = manager.determinePoisonStatus(
            relevantAddress = "0xzzz999qqq111",
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
            coinCode = "USDT",
            contractAddress = fakeContract,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun determinePoisonStatus_legitimateUsdc_returnsBlockchain() {
        val realContract = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        every { marketKit.token(TokenQuery(blockchainType, TokenType.Eip20(realContract))) } returns mockk<Token>()

        val result = manager.determinePoisonStatus(
            relevantAddress = "0xzzz999qqq111",
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
            coinCode = "USDC",
            contractAddress = realContract,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_nonStablecoinUnknownContract_returnsBlockchain() {
        val unknownContract = "0xunknown"
        every { marketKit.token(any<TokenQuery>()) } returns null

        val result = manager.determinePoisonStatus(
            relevantAddress = "0xzzz999qqq111",
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
            coinCode = "DAI",
            contractAddress = unknownContract,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    // --- Fix: fake stablecoin with null contract (TokenValue/unknown token) ---

    @Test
    fun determinePoisonStatus_usdtWithNullContractOnEvm_returnsSuspicious() {
        every { contactsRepository.getContactsFiltered(any(), any()) } returns emptyList()
        every { dao.get(any(), any(), any()) } returns null
        every { dao.getAllByType(any(), any(), any()) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = "0xscammer123456789abcdef",
            blockchainType = BlockchainType.Ethereum,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
            coinCode = "USDT",
            contractAddress = null,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun determinePoisonStatus_usdcWithNullContractOnBsc_returnsSuspicious() {
        every { contactsRepository.getContactsFiltered(any(), any()) } returns emptyList()
        every { dao.get(any(), any(), any()) } returns null
        every { dao.getAllByType(any(), any(), any()) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = "0xscammer123456789abcdef",
            blockchainType = BlockchainType.BinanceSmartChain,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
            coinCode = "usdc",
            contractAddress = null,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun determinePoisonStatus_usdtWithNullContractOnTon_returnsBlockchain() {
        every { contactsRepository.getContactsFiltered(any(), any()) } returns emptyList()
        every { dao.get(any(), any(), any()) } returns null
        every { dao.getAllByType(any(), any(), any()) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = "EQsometonaddress1234567",
            blockchainType = BlockchainType.Ton,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
            coinCode = "USDT",
            contractAddress = null,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_usdcWithNullContractOnStellar_returnsBlockchain() {
        every { contactsRepository.getContactsFiltered(any(), any()) } returns emptyList()
        every { dao.get(any(), any(), any()) } returns null
        every { dao.getAllByType(any(), any(), any()) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = "GBstellaraddress12345678",
            blockchainType = BlockchainType.Stellar,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
            coinCode = "USDC",
            contractAddress = null,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_usdtWithNullContractOnFantom_returnsSuspicious() {
        every { contactsRepository.getContactsFiltered(any(), any()) } returns emptyList()
        every { dao.get(any(), any(), any()) } returns null
        every { dao.getAllByType(any(), any(), any()) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = "0xfantomscammer123456789",
            blockchainType = BlockchainType.Fantom,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
            coinCode = "USDT",
            contractAddress = null,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    // --- isAddressSuspicious used for showCopyWarning in send/transaction details ---

    @Test
    fun isAddressSuspicious_similarToKnownOnTon_returnsTrue() {
        val knownAddress = "eqaaa_middle_bbb"
        val similarAddress = "eqaaa_poison_bbb"
        val tonUid = BlockchainType.Ton.uid
        every { dao.get(any(), tonUid, accountId) } returns null
        every { contactsRepository.getContactsFiltered(BlockchainType.Ton, addressQuery = similarAddress) } returns emptyList()
        every { dao.getAllByType(PoisonAddressType.KNOWN, tonUid, accountId) } returns
            listOf(PoisonAddress(knownAddress, tonUid, accountId, PoisonAddressType.KNOWN))

        assertTrue(manager.isAddressSuspicious(similarAddress, BlockchainType.Ton, accountId))
    }

    @Test
    fun isAddressSuspicious_unknownAddressNotSimilar_returnsFalse() {
        val knownAddress = "0xaaa_middle_bbb"
        val differentAddress = "0xzzz_middle_qqq"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid, accountId) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, accountId, PoisonAddressType.KNOWN))

        assertFalse(manager.isAddressSuspicious(differentAddress, blockchainType, accountId))
    }

    @Test
    fun isAddressSuspicious_scamOnTron_returnsTrue() {
        val address = "tscammer123456"
        val tronUid = BlockchainType.Tron.uid
        every { dao.get(address, tronUid, accountId) } returns
            PoisonAddress(address, tronUid, accountId, PoisonAddressType.SCAM)

        assertTrue(manager.isAddressSuspicious(address, BlockchainType.Tron, accountId))
    }

    @Test
    fun determinePoisonStatus_nonStablecoinWithNullContract_returnsBlockchain() {
        every { contactsRepository.getContactsFiltered(any(), any()) } returns emptyList()
        every { dao.get(any(), any(), any()) } returns null
        every { dao.getAllByType(any(), any(), any()) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = "0xsomeaddress123456789ab",
            blockchainType = blockchainType,
            accountId = accountId,
            isOutgoing = false,
            isCreatedByWallet = false,
            coinCode = "WETH",
            contractAddress = null,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    // --- getRelevantAddress: event-based address resolution respects direction ---

    @Test
    fun getPoisonStatus_outgoingEvmNoTo_prefersOutgoingEvents() {
        val poisonedRecipient = "0xpoisoned_recipient_addr"
        val senderAddr = "0xincoming_sender_address"
        every { dao.get(poisonedRecipient.lowercase(), blockchainUid, accountId) } returns
            PoisonAddress(poisonedRecipient.lowercase(), blockchainUid, accountId, PoisonAddressType.SCAM)

        val record = createEvmRecord(
            from = null,
            to = null,
            transactionRecordType = TransactionRecordType.EVM_OUTGOING,
            incomingEvents = listOf(TransferEvent(senderAddr, null, mockk(relaxed = true))),
            outgoingEvents = listOf(TransferEvent(poisonedRecipient, null, mockk(relaxed = true))),
            foreignTransaction = true,
        )

        assertEquals(PoisonStatus.SUSPICIOUS, manager.getPoisonStatus(record))
    }

    @Test
    fun getPoisonStatus_incomingEvmNoFrom_prefersIncomingEvents() {
        val senderAddr = "0xincoming_sender_address"
        val recipientAddr = "0xoutgoing_recipient_addr"
        every { dao.get(senderAddr.lowercase(), blockchainUid, accountId) } returns
            PoisonAddress(senderAddr.lowercase(), blockchainUid, accountId, PoisonAddressType.SCAM)

        val record = createEvmRecord(
            from = null,
            to = null,
            transactionRecordType = TransactionRecordType.EVM_INCOMING,
            incomingEvents = listOf(TransferEvent(senderAddr, null, mockk(relaxed = true))),
            outgoingEvents = listOf(TransferEvent(recipientAddr, null, mockk(relaxed = true))),
        )

        assertEquals(PoisonStatus.SUSPICIOUS, manager.getPoisonStatus(record))
    }

    @Test
    fun getPoisonStatus_outgoingEvmNoTo_fallsBackToIncomingWhenNoOutgoingEvents() {
        val senderAddr = "0xfallback_sender_address"
        every { dao.get(senderAddr.lowercase(), blockchainUid, accountId) } returns
            PoisonAddress(senderAddr.lowercase(), blockchainUid, accountId, PoisonAddressType.SCAM)

        val record = createEvmRecord(
            from = null,
            to = null,
            transactionRecordType = TransactionRecordType.EVM_OUTGOING,
            incomingEvents = listOf(TransferEvent(senderAddr, null, mockk(relaxed = true))),
            outgoingEvents = emptyList(),
            foreignTransaction = true,
        )

        assertEquals(PoisonStatus.SUSPICIOUS, manager.getPoisonStatus(record))
    }

    @Test
    fun getPoisonStatus_outgoingTronNoTo_prefersOutgoingEvents() {
        val poisonedRecipient = "tpoisoned_recipient_addr"
        val senderAddr = "tincoming_sender_address_"
        val tronUid = BlockchainType.Tron.uid
        every { dao.get(poisonedRecipient.lowercase(), tronUid, accountId) } returns
            PoisonAddress(poisonedRecipient.lowercase(), tronUid, accountId, PoisonAddressType.SCAM)
        every { contactsRepository.getContactsFiltered(BlockchainType.Tron, addressQuery = any()) } returns emptyList()
        every { dao.getAllByType(any(), tronUid, accountId) } returns emptyList()

        val record = createTronRecord(
            from = null,
            to = null,
            transactionRecordType = TransactionRecordType.TRON_OUTGOING,
            incomingEvents = listOf(TransferEvent(senderAddr, null, mockk(relaxed = true))),
            outgoingEvents = listOf(TransferEvent(poisonedRecipient, null, mockk(relaxed = true))),
            foreignTransaction = true,
        )

        assertEquals(PoisonStatus.SUSPICIOUS, manager.getPoisonStatus(record))
    }

    @Test
    fun getPoisonStatus_evmWithStandardTo_usesStandardAddress() {
        val standardTo = "0xstandard_to_address_aaa"
        every { dao.get(standardTo.lowercase(), blockchainUid, accountId) } returns
            PoisonAddress(standardTo.lowercase(), blockchainUid, accountId, PoisonAddressType.SCAM)

        val record = createEvmRecord(
            from = null,
            to = standardTo,
            transactionRecordType = TransactionRecordType.EVM_OUTGOING,
            incomingEvents = listOf(TransferEvent("0xother_addr", null, mockk(relaxed = true))),
            outgoingEvents = listOf(TransferEvent("0xother_addr", null, mockk(relaxed = true))),
            foreignTransaction = true,
        )

        // Standard to/from takes precedence over events
        assertEquals(PoisonStatus.SUSPICIOUS, manager.getPoisonStatus(record))
    }

    @Test
    fun getPoisonStatus_userCreatedEvmSwap_usesExchangeAddressAndReturnsCreated() {
        val record = createEvmRecord(
            transactionRecordType = TransactionRecordType.EVM_SWAP,
            exchangeAddress = "0xpancakeswap_router",
            foreignTransaction = false,
        )

        assertEquals(PoisonStatus.CREATED, manager.getPoisonStatus(record))
    }

    @Test
    fun getPoisonStatus_userCreatedEvmUnknownSwap_usesExchangeAddressAndReturnsCreated() {
        val record = createEvmRecord(
            transactionRecordType = TransactionRecordType.EVM_UNKNOWN_SWAP,
            exchangeAddress = "0xpancakeswap_router",
            foreignTransaction = false,
        )

        assertEquals(PoisonStatus.CREATED, manager.getPoisonStatus(record))
    }

    // --- Helpers ---

    private fun createEvmRecord(
        from: String? = null,
        to: String? = null,
        transactionRecordType: TransactionRecordType,
        incomingEvents: List<TransferEvent>? = null,
        outgoingEvents: List<TransferEvent>? = null,
        exchangeAddress: String? = null,
        foreignTransaction: Boolean = false,
    ): EvmTransactionRecord {
        val evmTransaction = mockk<io.horizontalsystems.ethereumkit.models.Transaction>(relaxed = true) {
            every { hashString } returns "0xhash123"
            every { transactionIndex } returns 0
            every { blockNumber } returns 100L
            every { timestamp } returns 1000L
            every { isFailed } returns false
        }
        val token = mockk<Token>(relaxed = true) {
            every { blockchainType } returns BlockchainType.Ethereum
        }
        val source = mockk<TransactionSource>(relaxed = true) {
            every { blockchain } returns mockk(relaxed = true) {
                every { type } returns BlockchainType.Ethereum
            }
            every { account } returns mockk(relaxed = true) {
                every { id } returns accountId
            }
        }
        return EvmTransactionRecord(
            from = from,
            to = to,
            transaction = evmTransaction,
            token = token,
            source = source,
            protected = false,
            transactionRecordType = transactionRecordType,
            incomingEvents = incomingEvents,
            outgoingEvents = outgoingEvents,
            exchangeAddress = exchangeAddress,
            foreignTransaction = foreignTransaction,
        )
    }

    private fun createTronRecord(
        from: String? = null,
        to: String? = null,
        transactionRecordType: TransactionRecordType,
        incomingEvents: List<TransferEvent>? = null,
        outgoingEvents: List<TransferEvent>? = null,
        foreignTransaction: Boolean = false,
    ): TronTransactionRecord {
        val tronTransaction = mockk<io.horizontalsystems.tronkit.models.Transaction>(relaxed = true) {
            every { hashString } returns "tronhash123"
            every { blockNumber } returns 200L
            every { timestamp } returns 2000000L
            every { isFailed } returns false
            every { fee } returns null
        }
        val token = mockk<Token>(relaxed = true) {
            every { blockchainType } returns BlockchainType.Tron
            every { decimals } returns 6
        }
        val source = mockk<TransactionSource>(relaxed = true) {
            every { blockchain } returns mockk(relaxed = true) {
                every { type } returns BlockchainType.Tron
            }
            every { account } returns mockk(relaxed = true) {
                every { id } returns accountId
            }
        }
        return TronTransactionRecord(
            from = from,
            to = to,
            token = token,
            source = source,
            transactionRecordType = transactionRecordType,
            incomingEvents = incomingEvents,
            outgoingEvents = outgoingEvents,
            transaction = tronTransaction,
            foreignTransaction = foreignTransaction,
        )
    }
}
