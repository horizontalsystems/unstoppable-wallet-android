package bitcoin.wallet.core.managers

import bitcoin.wallet.core.INetworkManager
import org.junit.Test
import org.mockito.Mockito.mock

class LoginManagerTest {

    val networkManager = mock(INetworkManager::class.java)
    val loginManager = LoginManager(networkManager)

    @Test
    fun login() {
        val words = listOf("1", "2")
        val pubKey = "tpub-asdasd"

        loginManager.login(words)

//        verify(networkManager).getJwtToken(pubKey)
    }
}