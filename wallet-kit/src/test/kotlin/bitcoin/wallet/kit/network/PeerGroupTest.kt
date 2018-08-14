package bitcoin.wallet.kit.network

import bitcoin.walllet.kit.network.MessageSender
import bitcoin.walllet.kit.network.PeerGroupListener
import bitcoin.walllet.kit.network.message.Message
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito.whenNew
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.net.SocketTimeoutException

@RunWith(PowerMockRunner::class)
@PrepareForTest(PeerGroup::class)

class PeerGroupTest {
    private lateinit var peerGroup: PeerGroup
    private lateinit var peerConnection: PeerConnection
    private lateinit var peerManager: PeerManager
    private lateinit var peerGroupListener: PeerGroupListener
    private val peerIp = "8.8.8.8"

    @Before
    fun setup() {
        peerGroupListener = mock(PeerGroupListener::class.java)
        peerManager = mock(PeerManager::class.java)
        peerConnection = mock(PeerConnection::class.java)
        peerGroup = PeerGroup(peerGroupListener, peerManager, 1)
    }

    @Test
    fun run() { // creates peer connection with given IP address
        whenever(peerManager.getPeerIp()).thenReturn(peerIp)
        whenNew(PeerConnection::class.java)
                .withArguments(peerIp, peerGroup)
                .thenReturn(peerConnection)

        peerGroup.start()

        Thread.sleep(500L)
        verify(peerConnection).start()

        // close thread:
        peerGroup.close()
        peerGroup.join()
    }

    @Test
    fun onMessage() { // passes message to message listener
        val sender = mock(MessageSender::class.java)
        val message = mock(Message::class.java)
        peerGroup.onMessage(sender, message)

        verify(peerGroupListener).onMessage(sender, message)
    }

    @Test
    fun disconnected() { // removes peer from connection list
        peerGroup.disconnected(peerIp, null)

        verify(peerManager).markSuccess(peerIp)
    }

    @Test
    fun disconnected_withError() { // removes peer from connection list
        peerGroup.disconnected(peerIp, SocketTimeoutException("Some Error"))

        verify(peerManager).markFailed(peerIp)
    }

}
