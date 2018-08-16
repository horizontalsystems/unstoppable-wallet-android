package bitcoin.wallet.kit.network

import bitcoin.walllet.kit.network.PeerGroupListener
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.net.SocketTimeoutException

@RunWith(PowerMockRunner::class)
@PrepareForTest(PeerGroup::class)

class PeerGroupTest {
    private lateinit var peerGroup: PeerGroup
    private lateinit var peer: Peer
    private lateinit var peerManager: PeerManager
    private lateinit var peerGroupListener: PeerGroupListener
    private val peerIp = "8.8.8.8"
    private val peerIp2 = "5.5.5.5"

    @Before
    fun setup() {
        peerGroupListener = mock(PeerGroupListener::class.java)
        peerManager = mock(PeerManager::class.java)
        peerGroup = PeerGroup(peerGroupListener, peerManager, 1)
        peer = mock(Peer::class.java)
        whenever(peer.host).thenReturn(peerIp)
    }

    @Test
    fun run() { // creates peer connection with given IP address
        whenever(peerManager.getPeerIp()).thenReturn(peerIp)
        whenNew(Peer::class.java)
                .withArguments(peerIp, peerGroup)
                .thenReturn(peer)

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
        peerGroup.disconnected(peer2, null)
        peerGroup.connected(peer)

        verify(peerGroupListener, times(2)).onReady()
    }

    @Test
    fun disconnected() { // removes peer from connection list
        peerGroup.disconnected(peer, null)

        verify(peerManager).markSuccess(peerIp)
    }

    @Test
    fun disconnected_withError() { // removes peer from connection list
        peerGroup.disconnected(peer, SocketTimeoutException("Some Error"))

        verify(peerManager).markFailed(peerIp)
    }
}
