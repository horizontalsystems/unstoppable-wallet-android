package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.TestHelper
import bitcoin.wallet.kit.messages.*
import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.models.InventoryItem
import bitcoin.wallet.kit.models.MerkleBlock
import bitcoin.wallet.kit.models.Transaction
import bitcoin.walllet.kit.io.BitcoinInput
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(Peer::class)

class PeerTest {

    private lateinit var peer: Peer
    private lateinit var listener: Peer.Listener
    private lateinit var peerConnection: PeerConnection
    private val network = MainNet()
    private val versionMessage_protocol_69000 = "F9BEB4D976657273696F6E0000000000660000008C490408880D01000D0400000000000061E5845B00000000000000000000000000000000000000000000FFFFD4707C06B26B0D04000000000000000000000000000000000000000000000000D8E6F92E8EC8F039112F5361746F7368693A302E31362E39392FE9380800"
    private val versionMessage_lastBlock_0 =    "F9BEB4D976657273696F6E000000000066000000BE39611E7F1101000D04000000000000D7E5845B00000000000000000000000000000000000000000000FFFFD4707C069DBE0D04000000000000000000000000000000000000000000000000E6030F56C7080373102F5361746F7368693A302E31362E322F0000000001"
    private val versionMessage_services_0 =     "F9BEB4D976657273696F6E00000000006600000041E561B07F110100000000000000000092E4845B00000000000000000000000000000000000000000000FFFFD4707C06B4670D04000000000000000000000000000000000000000000000000E343866042AF517C102F5361746F7368693A302E31362E322FE838080001"
    private val versionMessage_successful =     "F9BEB4D976657273696F6E000000000066000000600C82817F1101000D04000000000000D1DC845B00000000000000000000000000000000000000000000FFFFD4707C069ACD0D040000000000000000000000000000000000000000000000002A13586EB0756F44102F5361746F7368693A302E31362E322FE838080001"

    fun getMessageFromHex(hex: String): Message {
        val versionMessageRaw = TestHelper.hexToByteArray(hex)
        val bitcoinInput = BitcoinInput(versionMessageRaw)
        val parsedMsg = Message.Builder.parseMessage<Message>(bitcoinInput, network)
        return parsedMsg
    }

    @Before
    fun setup() {
        listener = mock(Peer.Listener::class.java)
        peerConnection = mock(PeerConnection::class.java)

        PowerMockito
                .whenNew(PeerConnection::class.java)
                .withAnyArguments()
                .thenReturn(peerConnection)

        peer = Peer("host", network, listener)
    }

    // when
    // we request merkle blocks
    // then
    // it sends getdata message to peer with the same list of hashes
    // and it becomes busy
    @Test
    fun requestMerkleBlocks() {
        val getDataMessage = mock(GetDataMessage::class.java)
        val headerHashes = arrayOf(
                "0000000000000005ed683decf91ff610c7710d03bb3f618d121d47cbcb1bc1e1".toByteArray(),
                "0000000000000027d222eb8a315aae9e437f907edd85ce9ec6ef4a3de3ee2d2f".toByteArray()
        )

        PowerMockito
                .whenNew(GetDataMessage::class.java)
                .withArguments(InventoryItem.MSG_FILTERED_BLOCK, headerHashes)
                .thenReturn(getDataMessage)

        peer.requestMerkleBlocks(headerHashes)

        verify(peerConnection).sendMessage(getDataMessage)
        Assert.assertFalse(peer.isFree)
    }

    // when
    // we request merkle blocks
    // and it is disconnected
    // then
    // it notifies us about error with list of all requested block hashes
    @Test
    fun requestMerkleBlocks_onFailedAll() {
        val anyException = Exception()

        val headerHashes = arrayOf(
                "0000000000000005ed683decf91ff610c7710d03bb3f618d121d47cbcb1bc1e1".toByteArray(),
                "0000000000000027d222eb8a315aae9e437f907edd85ce9ec6ef4a3de3ee2d2f".toByteArray()
        )

        peer.requestMerkleBlocks(headerHashes)
        peer.disconnected(anyException)

        verify(listener).disconnected(peer, anyException, headerHashes)
    }

    // when
    // we request merkle blocks
    // and it processes some of them
    // and it is disconnected
    // then
    // it notifies us about error with list of not processed block hashes
    @Test
    fun requestMerkleBlocks_onFailedOne() {
        val successMerkleBlockHash = "0000000000000005ed683decf91ff610c7710d03bb3f618d121d47cbcb1bc1e1".toByteArray()
        val failureMerkleBlockHash = "0000000000000027d222eb8a315aae9e437f907edd85ce9ec6ef4a3de3ee2d2f".toByteArray()

        val headerHashes = arrayOf(successMerkleBlockHash, failureMerkleBlockHash)

        val successMerkleBlock = mock(MerkleBlock::class.java)
        val successMerkleBlockMessage = mock(MerkleBlockMessage::class.java)

        whenever(successMerkleBlockMessage.merkleBlock).thenReturn(successMerkleBlock)
        whenever(successMerkleBlock.blockHash).thenReturn(successMerkleBlockHash)
        whenever(successMerkleBlock.associatedTransactionHashes).thenReturn(mutableListOf())

        peer.requestMerkleBlocks(headerHashes)
        peer.onMessage(successMerkleBlockMessage)
        peer.disconnected()

        verify(listener).disconnected(peer, null, arrayOf(failureMerkleBlockHash))
    }


    // when
    // it receives merkle block message with zero associated transactions
    // then
    // it notifies us about merkle block
    @Test
    fun onMessage_MerkleBlockMessage_withZeroTransaction() {
        val merkleBlock = mock(MerkleBlock::class.java)
        val merkleBlockMessage = mock(MerkleBlockMessage::class.java)

        whenever(merkleBlockMessage.merkleBlock).thenReturn(merkleBlock)
        whenever(merkleBlock.blockHash).thenReturn(byteArrayOf(1, 2))
        whenever(merkleBlock.associatedTransactionHashes).thenReturn(mutableListOf())

        peer.onMessage(merkleBlockMessage)

        verify(listener).onReceiveMerkleBlock(merkleBlock)
    }


    // given
    // it received merkle block message with one associated transaction
    // when
    // it receives transaction message associated with that merkle block
    // then
    // it adds transaction to merkle block
    // and it notifies us about merkle block
    @Test
    fun onMessage_MerkleBlockMessage_withOneTransaction() {
        // given
        val txHash = "d4a3974699818360b76bc62f4ec61d4a8ccb9abe161a24572644007bd85f2aaa".toByteArray()

        val merkleBlock = mock(MerkleBlock::class.java)
        val merkleBlockMessage = mock(MerkleBlockMessage::class.java)

        whenever(merkleBlockMessage.merkleBlock).thenReturn(merkleBlock)
        whenever(merkleBlock.associatedTransactionHashes).thenReturn(mutableListOf(txHash))

        peer.onMessage(merkleBlockMessage)

        // when
        val transaction = mock(Transaction::class.java)
        val txMessage = mock(TransactionMessage::class.java)

        whenever(txMessage.transaction).thenReturn(transaction)
        whenever(transaction.txHash).thenReturn(txHash)
        whenever(merkleBlock.associatedTransactions).thenReturn(mutableListOf(transaction))

        peer.onMessage(txMessage)

        // then
        verify(merkleBlock).addTransaction(transaction)
        verify(listener).onReceiveMerkleBlock(merkleBlock)
    }

    // given
    // we requested merkle block
    // when
    // it receives merkle block with zero transactions
    // then
    // it becomes free
    @Test
    fun requestMerkleBlocks_allCompleted() {
        // given
        val merkleBlockHash = "0000000000000005ed683decf91ff610c7710d03bb3f618d121d47cbcb1bc1e1".toByteArray()
        peer.requestMerkleBlocks(arrayOf(merkleBlockHash))

        // when
        val merkleBlock = mock(MerkleBlock::class.java)
        val merkleBlockMessage = mock(MerkleBlockMessage::class.java)

        whenever(merkleBlockMessage.merkleBlock).thenReturn(merkleBlock)
        whenever(merkleBlock.blockHash).thenReturn(merkleBlockHash)
        whenever(merkleBlock.associatedTransactionHashes).thenReturn(mutableListOf())

        peer.onMessage(merkleBlockMessage)

        // then
        Assert.assertTrue(peer.isFree)
    }

    @Test
    fun onMessage_transaction() {
        val transaction = mock(Transaction::class.java)
        val transactionMessage = mock(TransactionMessage::class.java)

        whenever(transactionMessage.transaction).thenReturn(transaction)

        peer.onMessage(transactionMessage)

        verify(listener).onReceiveTransaction(transaction)
    }

    @Test
    fun onMessage_inv_requestOnlyRequired() {
        val invVect1 = mock(InventoryItem::class.java)
        val invVect2 = mock(InventoryItem::class.java)

        val invMessage = InvMessage().apply {
            inventory = arrayOf(invVect1, invVect2)
        }

        whenever(listener.shouldRequest(invVect1)).thenReturn(true)
        whenever(listener.shouldRequest(invVect2)).thenReturn(false)

        peer.onMessage(invMessage)

        argumentCaptor<GetDataMessage>().apply {
            verify(peerConnection).sendMessage(capture())

            Assert.assertEquals(arrayOf(invVect1), firstValue.inventory)
        }
    }

    @Test
    fun onMessage_inv_newBlock() {
        val blockHash = "0000000000000005ed683decf91ff610c7710d03bb3f618d121d47cbcb1bc1e1".toByteArray()
        val invVectBlock = InventoryItem().apply {
            type = InventoryItem.MSG_BLOCK
            hash = blockHash
        }

        val invMessage = InvMessage().apply {
            inventory = arrayOf(invVectBlock)
        }

        whenever(listener.shouldRequest(invVectBlock)).thenReturn(true)

        peer.onMessage(invMessage)

        argumentCaptor<GetDataMessage>().apply {
            verify(peerConnection).sendMessage(capture())

            val invVect = firstValue.inventory.first()

            Assert.assertEquals(InventoryItem.MSG_FILTERED_BLOCK, invVect.type)
            Assert.assertEquals(blockHash, invVect.hash)
        }
    }

    @Test
    fun relayTransaction() {
        val transaction = mock(Transaction::class.java)
        val txHash = "0000000000000005ed683decf91ff610c7710d03bb3f618d121d47cbcb1bc1e1".toByteArray()

        whenever(transaction.txHash).thenReturn(txHash)

        peer.relay(transaction)

        argumentCaptor<InvMessage>().apply {

            verify(peerConnection).sendMessage(capture())

            val invVect = firstValue.inventory.first()

            Assert.assertEquals(InventoryItem.MSG_TX, invVect.type)
            Assert.assertEquals(txHash, invVect.hash)
        }
    }

    @Test
    fun onMessage_getData_tx() {

        val transaction = mock(Transaction::class.java)
        val txHash = "0000000000000005ed683decf91ff610c7710d03bb3f618d121d47cbcb1bc1e1".toByteArray()

        whenever(transaction.txHash).thenReturn(txHash)

        peer.relay(transaction)

        reset(peerConnection)

        val invVectTx = InventoryItem().apply {
            type = InventoryItem.MSG_TX
            hash = txHash
        }
        val getDataMessage = GetDataMessage(arrayOf(invVectTx))

        peer.onMessage(getDataMessage)

        argumentCaptor<TransactionMessage>().apply {
            verify(peerConnection).sendMessage(capture())

            Assert.assertEquals(firstValue.transaction, transaction)
        }
    }

    @Test
    fun onMessage_headers() {
        val headers = arrayOf(mock(Header::class.java))
        val headersMessage = HeadersMessage().apply {
            this.headers = headers
        }

        peer.onMessage(headersMessage)

        verify(listener).onReceiveHeaders(headers)
    }

    @Test
    fun onMessage_versionMessage_success() {
        val versionMessage = getMessageFromHex(versionMessage_successful)

        peer.onMessage(versionMessage)

        argumentCaptor<Message>().apply {
            verify(peerConnection).sendMessage(capture())

            Assert.assertTrue(firstValue is VerAckMessage)
        }
    }

    @Test
    fun onMessage_versionMessage_error_lastBlockIs0() {
        val versionMessage = getMessageFromHex(versionMessage_lastBlock_0)

        peer.onMessage(versionMessage)

        verify(peerConnection).close()
    }

    @Test
    fun onMessage_versionMessage_error_notFullNode() {
        val versionMessage = getMessageFromHex(versionMessage_services_0)

        peer.onMessage(versionMessage)

        verify(peerConnection).close()
    }

    @Test
    fun onMessage_versionMessage_error_notSupportingBloomFilter() {
        val versionMessage = getMessageFromHex(versionMessage_protocol_69000)

        peer.onMessage(versionMessage)

        verify(peerConnection).close()
    }

}
