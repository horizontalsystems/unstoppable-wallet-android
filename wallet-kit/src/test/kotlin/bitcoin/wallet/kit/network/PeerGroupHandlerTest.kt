package bitcoin.wallet.kit.network

import bitcoin.walllet.kit.network.MessageSender
import bitcoin.walllet.kit.network.message.GetHeadersMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(PeerGroupHandler::class)

class PeerGroupHandlerTest {

    private lateinit var messageSender: MessageSender
    private lateinit var peerGroupHandler: PeerGroupHandler

    @Before
    fun setup() {
        messageSender = mock(MessageSender::class.java)
        peerGroupHandler = PeerGroupHandler()
    }

    @Test
    fun onReady() {
        val getHeaderMessage = mock(GetHeadersMessage::class.java)

        PowerMockito.whenNew(GetHeadersMessage::class.java)
                .withNoArguments()
                .thenReturn(getHeaderMessage)

        peerGroupHandler.onReady()

        // pass control too syncer
    }

}
