package bitcoin.wallet.kit.network

import bitcoin.wallet.kit.blocks.MerkleBlock
import bitcoin.wallet.kit.message.MerkleBlockMessage
import bitcoin.wallet.kit.message.TransactionMessage
import bitcoin.walllet.kit.network.message.GetDataMessage
import bitcoin.walllet.kit.network.message.InvMessage
import bitcoin.walllet.kit.struct.InvVect
import bitcoin.walllet.kit.struct.Transaction
import com.nhaarman.mockito_kotlin.argumentCaptor
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
import javax.xml.bind.DatatypeConverter

@RunWith(PowerMockRunner::class)
@PrepareForTest(Peer::class)

class PeerTest {

    private lateinit var peer: Peer
    private lateinit var listener: Peer.Listener
    private lateinit var peerConnection: PeerConnection

    @Before
    fun setup() {
        listener = mock(Peer.Listener::class.java)
        peerConnection = mock(PeerConnection::class.java)

        PowerMockito
                .whenNew(PeerConnection::class.java)
                .withAnyArguments()
                .thenReturn(peerConnection)

        peer = Peer("host", listener)
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
                DatatypeConverter.parseHexBinary("0000000000000005ed683decf91ff610c7710d03bb3f618d121d47cbcb1bc1e1"),
                DatatypeConverter.parseHexBinary("0000000000000027d222eb8a315aae9e437f907edd85ce9ec6ef4a3de3ee2d2f")
        )

        PowerMockito
                .whenNew(GetDataMessage::class.java)
                .withArguments(InvVect.MSG_FILTERED_BLOCK, headerHashes)
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
                DatatypeConverter.parseHexBinary("0000000000000005ed683decf91ff610c7710d03bb3f618d121d47cbcb1bc1e1"),
                DatatypeConverter.parseHexBinary("0000000000000027d222eb8a315aae9e437f907edd85ce9ec6ef4a3de3ee2d2f")
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
        val successMerkleBlockHash = DatatypeConverter.parseHexBinary("0000000000000005ed683decf91ff610c7710d03bb3f618d121d47cbcb1bc1e1")
        val failureMerkleBlockHash = DatatypeConverter.parseHexBinary("0000000000000027d222eb8a315aae9e437f907edd85ce9ec6ef4a3de3ee2d2f")

        val headerHashes = arrayOf(successMerkleBlockHash, failureMerkleBlockHash)

        val successMerkleBlock = mock(MerkleBlock::class.java)
        val successMerkleBlockMessage = mock(MerkleBlockMessage::class.java)

        whenever(successMerkleBlockMessage.merkleBlock).thenReturn(successMerkleBlock)
        whenever(successMerkleBlock.blockHash).thenReturn(successMerkleBlockHash)
        whenever(successMerkleBlock.associatedTransactionHashes).thenReturn(arrayOf())

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
        whenever(merkleBlock.associatedTransactionHashes).thenReturn(arrayOf())

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
        val txHash = DatatypeConverter.parseHexBinary("d4a3974699818360b76bc62f4ec61d4a8ccb9abe161a24572644007bd85f2aaa")

        val merkleBlock = mock(MerkleBlock::class.java)
        val merkleBlockMessage = mock(MerkleBlockMessage::class.java)

        whenever(merkleBlockMessage.merkleBlock).thenReturn(merkleBlock)
        whenever(merkleBlock.associatedTransactionHashes).thenReturn(arrayOf(txHash))

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
        val merkleBlockHash = DatatypeConverter.parseHexBinary("0000000000000005ed683decf91ff610c7710d03bb3f618d121d47cbcb1bc1e1")
        peer.requestMerkleBlocks(arrayOf(merkleBlockHash))

        // when
        val merkleBlock = mock(MerkleBlock::class.java)
        val merkleBlockMessage = mock(MerkleBlockMessage::class.java)

        whenever(merkleBlockMessage.merkleBlock).thenReturn(merkleBlock)
        whenever(merkleBlock.blockHash).thenReturn(merkleBlockHash)
        whenever(merkleBlock.associatedTransactionHashes).thenReturn(arrayOf())

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
        val invVect1 = mock(InvVect::class.java)
        val invVect2 = mock(InvVect::class.java)

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
        val blockHash = DatatypeConverter.parseHexBinary("0000000000000005ed683decf91ff610c7710d03bb3f618d121d47cbcb1bc1e1")
        val invVectBlock = InvVect().apply {
            type = InvVect.MSG_BLOCK
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

            Assert.assertEquals(InvVect.MSG_FILTERED_BLOCK, invVect.type)
            Assert.assertEquals(blockHash, invVect.hash)
        }
    }

}
