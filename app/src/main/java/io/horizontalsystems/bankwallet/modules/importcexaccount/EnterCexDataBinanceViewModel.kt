package cash.p.terminal.modules.importcexaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.entities.AccountOrigin
import cash.p.terminal.entities.AccountType
import cash.p.terminal.entities.CexType

class EnterCexDataBinanceViewModel : ViewModel() {
    private val accountManager = App.accountManager
    private val accountFactory = App.accountFactory
    private val gson = Gson()

    private var apiKey: String? = null
    private var secretKey: String? = null
    private var connectEnabled = false
    private var accountCreated = false
    private var errorMessage: String? = null

    var uiState by mutableStateOf(UiState(connectEnabled, accountCreated, apiKey, secretKey, errorMessage))
        private set

    fun onEnterApiKey(v: String) {
        apiKey = v
        emitState()
    }

    fun onEnterSecretKey(v: String) {
        secretKey = v
        emitState()
    }

    fun onScannedData(data: String) {
        val apiCredentials = try {
            gson.fromJson(data, BinanceCexApiCredentials::class.java)
        } catch (error: Throwable) {
            null
        }

        val scannedApiKey = apiCredentials?.apiKey
        val scannedSecretKey = apiCredentials?.secretKey
        if (scannedApiKey.isNullOrBlank() || scannedSecretKey.isNullOrBlank()) {
            apiKey = null
            secretKey = null

            errorMessage = Translator.getString(R.string.WalletConnect_Error_DataParsingError)
        } else {
            apiKey = scannedApiKey
            secretKey = scannedSecretKey

            errorMessage = null
        }

        emitState()
    }

    private fun emitState() {
        connectEnabled = !(apiKey.isNullOrBlank() || secretKey.isNullOrBlank())
        uiState = UiState(connectEnabled, accountCreated, apiKey, secretKey, errorMessage)
    }

    fun onClickConnect() {
        val tmpApiKey = apiKey ?: return
        val tmpSecretKey = secretKey ?: return
        val cexType = CexType.Binance(tmpApiKey, tmpSecretKey)

        val account = accountFactory.account("Binance Wallet", AccountType.Cex(cexType = cexType), AccountOrigin.Restored, true, false)

        accountManager.save(account)

        accountCreated = true

        emitState()
    }

    data class BinanceCexApiCredentials(
        val apiKey: String?,
        val secretKey: String?,
        val comment: String?
    )

    class UiState(
        val connectEnabled: Boolean,
        val accountCreated: Boolean,
        val apiKey: String?,
        val secretKey: String?,
        val errorMessage: String?
    )
}
