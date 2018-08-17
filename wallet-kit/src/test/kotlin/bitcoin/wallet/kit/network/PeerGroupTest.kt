package bitcoin.wallet.kit.network

import bitcoin.walllet.kit.network.PeerGroupListener
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.net.SocketTimeoutException

@RunWith(PowerMockRunner::class)
@PrepareForTest(PeerGroup::class)

class PeerGroupTest {
    private lateinit var peerGroup: PeerGroup
    private lateinit var peer: Peer
    private lateinit var peer2: Peer
    private lateinit var peerManager: PeerManager
    private lateinit var peerGroupListener: PeerGroupListener
    private val peerIp = "8.8.8.8"
    private val peerIp2 = "5.5.5.5"

    @Before
    fun setup() {
        peerGroupListener = mock(PeerGroupListener::class.java)
        peerManager = mock(PeerManager::class.java)
        peerGroup = PeerGroup(peerGroupListener, peerManager, 2)
        peer = mock(Peer::class.java)
        peer2 = mock(Peer::class.java)
        whenever(peer.host).thenReturn(peerIp)

        whenever(peerManager.getPeerIp())
                .thenReturn(peerIp, peerIp2)

        PowerMockito.whenNew(Peer::class.java)
                .withAnyArguments()
                .thenReturn(peer, peer2)
    }

    @Test
    fun run() { // creates peer connection with given IP address
        peerGroup.start()

        Thread.sleep(500L)
        verify(peer).start()

        // close thread:
        peerGroup.close()
        peerGroup.join()
    }

    @Test
    fun requestHeaders() {
        val hashes = arrayOf<ByteArray>()
        peerGroup.connected(peer)
        peerGroup.requestHeaders(hashes)

        verify(peer).requestHeaders(hashes)
    }

    @Test
    fun requestBlocks() {
        // we should be able to set field instead of stubbing value
        whenever(peer.isFree).thenReturn(true)

        peerGroup.start()
        peerGroup.connected(peer)

        val hashes = arrayOf(
                byteArrayOf(1, 2),
                byteArrayOf(3, 4)
        )

        peerGroup.requestMerkleBlocks(hashes)
        verify(peer).requestBlocks(hashes)
    }

    @Test
    fun requestBlocks_two_peer() {
        // we should be able to set field instead of stubbing value
        whenever(peer.isFree).thenReturn(true, false)
        whenever(peer2.isFree).thenReturn(true)

        peerGroup.start()
        peerGroup.connected(peer)

        Thread.sleep(2001L) // wait for second peer connection

        val hashes = arrayOf(
                byteArrayOf(1),
                byteArrayOf(2),
                byteArrayOf(3),
                byteArrayOf(4),
                byteArrayOf(5),
                byteArrayOf(6),
                byteArrayOf(7),
                byteArrayOf(8),
                byteArrayOf(9),
                byteArrayOf(10),
                byteArrayOf(11),
                byteArrayOf(12))

        peerGroup.requestMerkleBlocks(hashes)

        verify(peer).requestBlocks(hashes.copyOfRange(0, 10))
        verify(peer2).requestBlocks(hashes.copyOfRange(10, 12))
    }

    @Test
    fun connected_onReady() {
        peerGroup.connected(peer)
        verify(peerGroupListener).onReady()
    }

    @Test
    fun connected_onReady_once() {
        val peer2 = mock(Peer::class.java)
        whenever(peer2.host).thenReturn(peerIp2)

        peerGroup.connected(peer)
        peerGroup.connected(peer2)
        verify(peerGroupListener).onReady()
    }

    @Test
    fun connected_onReady_twice() {
        val peer2 = mock(Peer::class.java)
        whenever(peer2.host).thenReturn(peerIp2)

        peerGroup.connected(peer2)
        peerGroup.disconnected(peer2, null, arrayOf())
        peerGroup.connected(peer)

        verify(peerGroupListener, times(2)).onReady()
    }

    @Test
    fun disconnected() { // removes peer from connection list
        peerGroup.disconnected(peer, null, arrayOf())

        verify(peerManager).markSuccess(peerIp)
    }

    @Test
    fun disconnected_withError() { // removes peer from connection list
        peerGroup.disconnected(peer, SocketTimeoutException("Some Error"), arrayOf())

        verify(peerManager).markFailed(peerIp)
    }
}
