package cash.p.terminal.core.managers

import cash.p.terminal.core.storage.PoisonAddressDao
import cash.p.terminal.entities.PoisonAddress
import cash.p.terminal.entities.PoisonAddressType
import cash.p.terminal.modules.contacts.ContactsRepository
import cash.p.terminal.modules.contacts.model.Contact
import cash.p.terminal.modules.transactions.poison_status.PoisonStatus
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
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

class PoisonAddressManagerTest {

    private val dao = mockk<PoisonAddressDao>(relaxed = true)
    private val contactsRepository = mockk<ContactsRepository>()
    private val marketKit = mockk<MarketKitWrapper>()
    private val blockchainType = BlockchainType.Ethereum
    private val blockchainUid = blockchainType.uid

    private lateinit var manager: PoisonAddressManager

    @Before
    fun setup() {
        every { contactsRepository.getContactsFiltered(any(), any(), any()) } returns emptyList()
        every { dao.get(any(), any()) } returns null
        every { dao.getAllByType(any(), any()) } returns emptyList()
        every { marketKit.token(any<TokenQuery>()) } returns null

        manager = PoisonAddressManager(dao, contactsRepository, marketKit)
    }

    // --- determinePoisonStatus ---

    @Test
    fun determinePoisonStatus_nullAddress_returnsBlockchain() {
        val result = manager.determinePoisonStatus(
            relevantAddress = null,
            blockchainType = blockchainType,
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
            isOutgoing = true,
            isCreatedByWallet = true,
        )
        assertEquals(PoisonStatus.CREATED, result)
    }

    @Test
    fun determinePoisonStatus_knownAddress_returnsBlockchain() {
        val address = "0xabc123def456"
        every { dao.get(address, blockchainUid) } returns
            PoisonAddress(address, blockchainUid, PoisonAddressType.KNOWN)

        val result = manager.determinePoisonStatus(
            relevantAddress = address,
            blockchainType = blockchainType,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_scamAddress_returnsSuspicious() {
        val address = "0xabc123def456"
        every { dao.get(address, blockchainUid) } returns
            PoisonAddress(address, blockchainUid, PoisonAddressType.SCAM)

        val result = manager.determinePoisonStatus(
            relevantAddress = address,
            blockchainType = blockchainType,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun determinePoisonStatus_similarToKnown_returnsSuspicious() {
        val knownAddress = "0xaaaa_middle_bbb"
        val similarAddress = "0xa_different_bbb"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, PoisonAddressType.KNOWN))

        val result = manager.determinePoisonStatus(
            relevantAddress = similarAddress,
            blockchainType = blockchainType,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun determinePoisonStatus_notSimilarToKnown_returnsBlockchain() {
        val knownAddress = "0xaaaa_middle_bbb"
        val differentAddress = "0xz_different_zzz"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, PoisonAddressType.KNOWN))

        val result = manager.determinePoisonStatus(
            relevantAddress = differentAddress,
            blockchainType = blockchainType,
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
        every { dao.get(lowerAddress, blockchainUid) } returns
            PoisonAddress(lowerAddress, blockchainUid, PoisonAddressType.SCAM)

        val result = manager.determinePoisonStatus(
            relevantAddress = mixedCaseAddress,
            blockchainType = blockchainType,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun saveKnownAddress_storesLowercased() {
        val mixedCase = "0xAbCdEf123456"
        manager.saveKnownAddress(mixedCase, blockchainType)

        verify {
            dao.insert(
                PoisonAddress("0xabcdef123456", blockchainUid, PoisonAddressType.KNOWN)
            )
        }
    }

    // --- Similarity edge cases ---

    @Test
    fun determinePoisonStatus_similarFirst3Last3Match_returnsSuspicious() {
        val knownAddress = "abc_known_xyz"
        val similarAddress = "abc_poison_xyz"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, PoisonAddressType.KNOWN))

        val result = manager.determinePoisonStatus(
            relevantAddress = similarAddress,
            blockchainType = blockchainType,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun determinePoisonStatus_onlyFirst3Match_returnsBlockchain() {
        val knownAddress = "abc_known_xyz"
        val partialMatch = "abc_poison_qqq"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, PoisonAddressType.KNOWN))

        val result = manager.determinePoisonStatus(
            relevantAddress = partialMatch,
            blockchainType = blockchainType,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_sameAddressAsKnown_returnsBlockchain() {
        val address = "abc_same_xyz"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid) } returns
            listOf(PoisonAddress(address, blockchainUid, PoisonAddressType.KNOWN))

        val result = manager.determinePoisonStatus(
            relevantAddress = address,
            blockchainType = blockchainType,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    @Test
    fun determinePoisonStatus_shortAddress_returnsBlockchain() {
        val shortAddress = "abcde"  // length 5 < SIMILARITY_CHARS * 2 = 6
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid) } returns
            listOf(PoisonAddress("abcxe", blockchainUid, PoisonAddressType.KNOWN))

        val result = manager.determinePoisonStatus(
            relevantAddress = shortAddress,
            blockchainType = blockchainType,
            isOutgoing = false,
            isCreatedByWallet = false,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }

    // --- isAddressSuspicious ---

    @Test
    fun isAddressSuspicious_knownAddress_returnsFalse() {
        val address = "0xabc123def456"
        every { dao.get(address, blockchainUid) } returns
            PoisonAddress(address, blockchainUid, PoisonAddressType.KNOWN)

        assertFalse(manager.isAddressSuspicious(address, blockchainType))
    }

    @Test
    fun isAddressSuspicious_scamAddress_returnsTrue() {
        val address = "0xabc123def456"
        every { dao.get(address, blockchainUid) } returns
            PoisonAddress(address, blockchainUid, PoisonAddressType.SCAM)

        assertTrue(manager.isAddressSuspicious(address, blockchainType))
    }

    @Test
    fun isAddressSuspicious_similarToKnown_returnsTrue() {
        val knownAddress = "abc_known_xyz"
        val similarAddress = "abc_poison_xyz"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, PoisonAddressType.KNOWN))

        assertTrue(manager.isAddressSuspicious(similarAddress, blockchainType))
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

        assertFalse(manager.isAddressSuspicious(address, blockchainType))
    }

    @Test
    fun isAddressSuspicious_nullAddress_returnsFalse() {
        assertFalse(manager.isAddressSuspicious(null, blockchainType))
    }

    // --- Zero-amount outgoing ---

    @Test
    fun determinePoisonStatus_zeroAmountOutgoing_returnsSuspicious() {
        val result = manager.determinePoisonStatus(
            relevantAddress = "0xabc123def456",
            blockchainType = blockchainType,
            isOutgoing = true,
            isCreatedByWallet = false,
            amount = BigDecimal.ZERO,
        )
        assertEquals(PoisonStatus.SUSPICIOUS, result)
    }

    @Test
    fun determinePoisonStatus_nonZeroAmountOutgoing_notSuspiciousByAmount() {
        val result = manager.determinePoisonStatus(
            relevantAddress = "0xzzz999qqq111",
            blockchainType = blockchainType,
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
        every { dao.get(any(), any()) } returns null
        every { dao.getAllByType(any(), any()) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = "0xscammer123456789abcdef",
            blockchainType = BlockchainType.Ethereum,
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
        every { dao.get(any(), any()) } returns null
        every { dao.getAllByType(any(), any()) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = "0xscammer123456789abcdef",
            blockchainType = BlockchainType.BinanceSmartChain,
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
        every { dao.get(any(), any()) } returns null
        every { dao.getAllByType(any(), any()) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = "EQsometonaddress1234567",
            blockchainType = BlockchainType.Ton,
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
        every { dao.get(any(), any()) } returns null
        every { dao.getAllByType(any(), any()) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = "GBstellaraddress12345678",
            blockchainType = BlockchainType.Stellar,
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
        every { dao.get(any(), any()) } returns null
        every { dao.getAllByType(any(), any()) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = "0xfantomscammer123456789",
            blockchainType = BlockchainType.Fantom,
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
        every { dao.get(any(), tonUid) } returns null
        every { contactsRepository.getContactsFiltered(BlockchainType.Ton, addressQuery = similarAddress) } returns emptyList()
        every { dao.getAllByType(PoisonAddressType.KNOWN, tonUid) } returns
            listOf(PoisonAddress(knownAddress, tonUid, PoisonAddressType.KNOWN))

        assertTrue(manager.isAddressSuspicious(similarAddress, BlockchainType.Ton))
    }

    @Test
    fun isAddressSuspicious_unknownAddressNotSimilar_returnsFalse() {
        val knownAddress = "0xaaa_middle_bbb"
        val differentAddress = "0xzzz_middle_qqq"
        every { dao.getAllByType(PoisonAddressType.KNOWN, blockchainUid) } returns
            listOf(PoisonAddress(knownAddress, blockchainUid, PoisonAddressType.KNOWN))

        assertFalse(manager.isAddressSuspicious(differentAddress, blockchainType))
    }

    @Test
    fun isAddressSuspicious_scamOnTron_returnsTrue() {
        val address = "tscammer123456"
        val tronUid = BlockchainType.Tron.uid
        every { dao.get(address, tronUid) } returns
            PoisonAddress(address, tronUid, PoisonAddressType.SCAM)

        assertTrue(manager.isAddressSuspicious(address, BlockchainType.Tron))
    }

    @Test
    fun determinePoisonStatus_nonStablecoinWithNullContract_returnsBlockchain() {
        every { contactsRepository.getContactsFiltered(any(), any()) } returns emptyList()
        every { dao.get(any(), any()) } returns null
        every { dao.getAllByType(any(), any()) } returns emptyList()

        val result = manager.determinePoisonStatus(
            relevantAddress = "0xsomeaddress123456789ab",
            blockchainType = blockchainType,
            isOutgoing = false,
            isCreatedByWallet = false,
            coinCode = "WETH",
            contractAddress = null,
        )
        assertEquals(PoisonStatus.BLOCKCHAIN, result)
    }
}
