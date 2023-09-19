package io.horizontalsystems.bankwallet.modules.importcexaccount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.binance.connector.client.exceptions.BinanceClientException
import com.binance.connector.client.exceptions.BinanceConnectorException
import com.google.gson.Gson
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.BinanceCexProvider
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CexType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EnterCexDataBinanceViewModel : ViewModel() {
    private val accountManager = App.accountManager
    private val accountFactory = App.accountFactory
    private val gson = Gson()

    private var apiKey: String? = null
    private var secretKey: String? = null
    private var connectEnabled = false
    private var accountCreated = false
    private var errorMessage: String? = null
    private var showSpinner = false

    var uiState by mutableStateOf(
        UiState(
            connectEnabled = connectEnabled,
            accountCreated = accountCreated,
            apiKey = apiKey,
            secretKey = secretKey,
            errorMessage = errorMessage,
            showSpinner = showSpinner
        )
    )
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
        viewModelScope.launch {
            uiState = UiState(
                connectEnabled = connectEnabled,
                accountCreated = accountCreated,
                apiKey = apiKey,
                secretKey = secretKey,
                errorMessage = errorMessage,
                showSpinner = showSpinner
            )
        }
    }

    fun onClickConnect() {
        val tmpApiKey = apiKey ?: return
        val tmpSecretKey = secretKey ?: return
        showSpinner = true
        errorMessage = null
        emitState()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                BinanceCexProvider.validate(tmpApiKey, tmpSecretKey)
                createAccount(tmpApiKey, tmpSecretKey)
            } catch (error: BinanceClientException) {
                errorMessage = Translator.getString(R.string.Cex_Error_FailedToConnectApiKey)
            } catch (error: BinanceConnectorException) {
                errorMessage = Translator.getString(R.string.Hud_Text_NoInternet)
            }
            showSpinner = false
            emitState()
        }
    }

    private fun createAccount(binanceApiKey: String, binanceSecretKey: String) {
        val cexType = CexType.Binance(binanceApiKey, binanceSecretKey)
        val name = accountFactory.getNextCexAccountName(cexType)

        val account = accountFactory.account(
            name,
            AccountType.Cex(cexType = cexType),
            AccountOrigin.Restored,
            true,
            false,
        )

        accountManager.save(account)

        accountCreated = true
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
        val errorMessage: String?,
        val showSpinner: Boolean
    )
}
