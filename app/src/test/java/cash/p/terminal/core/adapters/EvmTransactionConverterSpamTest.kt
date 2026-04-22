package cash.p.terminal.core.adapters

import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.data.repository.EvmTransactionRepository
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.erc20kit.events.TokenInfo
import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.core.storage.TransactionSyncSourceStorage
import io.horizontalsystems.ethereumkit.crypto.InternalBouncyCastleProvider
import io.horizontalsystems.ethereumkit.decorations.UnknownTransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigInteger
import java.security.Security

/**
 * Regression test for MOBILE-623: zero-value token-event spam must be marked as spam so
 * that Hide Suspicious Transactions can filter it. Covers two phantom patterns:
 *   1. Transaction.to == null (Erc20TransactionSyncer phantom) -> EVM_EXTERNAL_CONTRACT_CALL
 *   2. Transaction.to == contract but method is unknown -> EVM_CONTRACT_CALL
 */
class EvmTransactionConverterSpamTest {

    companion object {
        init {
            // CryptoUtils.sha3 needs ETH-KECCAK-256 from BouncyCastle, which EthereumKit
            // registers at runtime. Unit tests must register it manually before
            // TransferEventInstance.<clinit> triggers the keccak computation.
            Security.addProvider(InternalBouncyCastleProvider.getInstance())
        }
    }

    private val bscBlockchain = Blockchain(BlockchainType.BinanceSmartChain, "Binance Smart Chain", null)
    private val userAddress = Address("0x1111111111111111111111111111111111111111")
    private val phantomRecipient = Address("0x2222222222222222222222222222222222222222")
    private val realSender = Address("0x3333333333333333333333333333333333333333")
    private val usdtContract = Address("0x55d398326f99059ff775485246999027b3197955")

    private val baseToken = Token(
        coin = Coin(uid = "binancecoin", name = "BNB", code = "BNB"),
        blockchain = bscBlockchain,
        type = TokenType.Native,
        decimals = 18
    )

    private val usdtToken = Token(
        coin = Coin(uid = "tether", name = "Tether", code = "USDT"),
        blockchain = bscBlockchain,
        type = TokenType.Eip20(usdtContract.hex),
        decimals = 18
    )

    private val source = TransactionSource(
        blockchain = bscBlockchain,
        account = mockk<Account>(relaxed = true),
        meta = null
    )

    private val repository: EvmTransactionRepository = mockk(relaxed = true)
    private val coinManager: ICoinManager = mockk(relaxed = true)
    private val evmLabelManager: EvmLabelManager = mockk(relaxed = true)
    private val syncSourceStorage: TransactionSyncSourceStorage = mockk(relaxed = true)

    private fun createConverter(): EvmTransactionConverter {
        every { repository.receiveAddress } returns userAddress
        every { repository.getBlockchainType() } returns BlockchainType.BinanceSmartChain
        every {
            coinManager.getToken(TokenQuery.eip20(BlockchainType.BinanceSmartChain, usdtContract.hex))
        } returns usdtToken
        every { syncSourceStorage.getSource(any()) } returns null

        return EvmTransactionConverter(
            coinManager = coinManager,
            evmTransactionRepository = repository,
            source = source,
            baseToken = baseToken,
            evmLabelManager = evmLabelManager,
            syncSourceStorage = syncSourceStorage
        )
    }

    private fun phantomTransaction(from: Address, hashByte: Byte = 1): Transaction = Transaction(
        hash = ByteArray(32) { hashByte },
        timestamp = 1_700_000_000L,
        isFailed = false,
        from = from,
        to = null,
        value = BigInteger.ZERO
    )

    private fun contractCallTransaction(
        to: Address = usdtContract,
        value: BigInteger = BigInteger.ZERO,
        input: ByteArray? = null,
        hashByte: Byte = 3
    ): Transaction = Transaction(
        hash = ByteArray(32) { hashByte },
        timestamp = 1_700_000_000L,
        isFailed = false,
        from = userAddress,
        to = to,
        value = value,
        input = input
    )

    private fun decorationWithEvent(
        from: Address,
        to: Address,
        value: BigInteger
    ) = UnknownTransactionDecoration(
        from,
        null,
        userAddress,
        BigInteger.ZERO,
        emptyList(),
        listOf(
            TransferEventInstance(
                contractAddress = usdtContract,
                from = from,
                to = to,
                value = value,
                tokenInfo = TokenInfo("Tether USD", "USDT", 18)
            )
        )
    )

    @Test
    fun transactionRecord_phantomZeroValueOutgoingWithNullTo_isExternalContractCallAndSpam() {
        val transaction = phantomTransaction(from = userAddress)
        val decoration = decorationWithEvent(
            from = userAddress,
            to = phantomRecipient,
            value = BigInteger.ZERO
        )

        val record = createConverter().transactionRecord(FullTransaction(transaction, decoration, emptyMap()))

        assertEquals(TransactionRecordType.EVM_EXTERNAL_CONTRACT_CALL, record.transactionRecordType)
        assertTrue("Phantom zero-value USDT transfer must be marked as spam", record.spam)
    }

    @Test
    fun transactionRecord_phantomNonZeroIncomingWithNullTo_isExternalContractCallAndNotSpam() {
        val transaction = phantomTransaction(from = realSender, hashByte = 2)
        val decoration = decorationWithEvent(
            from = realSender,
            to = userAddress,
            value = BigInteger("1000000000000000000")
        )

        val record = createConverter().transactionRecord(FullTransaction(transaction, decoration, emptyMap()))

        assertEquals(TransactionRecordType.EVM_EXTERNAL_CONTRACT_CALL, record.transactionRecordType)
        assertFalse("Legitimate non-zero USDT transfer must not be marked as spam", record.spam)
    }

    @Test
    fun transactionRecord_zeroValueContractCallNoMethod_isContractCallAndSpam() {
        val transaction = contractCallTransaction(hashByte = 3)
        val decoration = decorationWithEvent(
            from = userAddress,
            to = phantomRecipient,
            value = BigInteger.ZERO
        )

        val record = createConverter().transactionRecord(FullTransaction(transaction, decoration, emptyMap()))

        assertEquals(TransactionRecordType.EVM_CONTRACT_CALL, record.transactionRecordType)
        assertTrue("Zero-value USDT contract call with unknown method must be marked as spam", record.spam)
    }

    @Test
    fun transactionRecord_nonZeroContractCallNoMethod_isContractCallAndNotSpam() {
        val transaction = contractCallTransaction(hashByte = 4)
        val decoration = decorationWithEvent(
            from = userAddress,
            to = phantomRecipient,
            value = BigInteger("1000000000000000000")
        )

        val record = createConverter().transactionRecord(FullTransaction(transaction, decoration, emptyMap()))

        assertEquals(TransactionRecordType.EVM_CONTRACT_CALL, record.transactionRecordType)
        assertFalse("Non-zero USDT contract call must not be marked as spam", record.spam)
    }

    @Test
    fun transactionRecord_zeroValueContractCallWithKnownMethod_isContractCallAndNotSpam() {
        val transaction = contractCallTransaction(input = ByteArray(4), hashByte = 5)
        val decoration = decorationWithEvent(
            from = userAddress,
            to = phantomRecipient,
            value = BigInteger.ZERO
        )
        every { evmLabelManager.methodLabel(any()) } returns "transfer"

        val record = createConverter().transactionRecord(FullTransaction(transaction, decoration, emptyMap()))

        assertEquals(TransactionRecordType.EVM_CONTRACT_CALL, record.transactionRecordType)
        assertFalse("Contract call with a recognized method must not be marked as spam", record.spam)
    }
}
