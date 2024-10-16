package cash.p.terminal.modules.tonconnect

import androidx.lifecycle.viewModelScope
import com.tonapps.wallet.data.tonconnect.entities.DAppEntity
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import cash.p.terminal.core.App
import cash.p.terminal.core.ViewModelUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TonConnectListViewModel : ViewModelUiState<TonConnectListUiState>() {

    private val tonConnectKit = App.tonConnectManager.kit

    private var dapps: List<DAppEntity> = listOf()
    private var dAppRequestEntity: DAppRequestEntity? = null
    private var error: Throwable? = null

    override fun createState() = TonConnectListUiState(
        dapps = dapps,
        dAppRequestEntity = dAppRequestEntity,
        error = error
    )

    init {
        viewModelScope.launch {
            tonConnectKit.getDApps().collect {
                dapps = it
                emitState()
            }
        }
    }

    fun setConnectionUri(v: String) {
        error = null

        try {
            dAppRequestEntity = tonConnectKit.readData(v)
        } catch (e: Throwable) {
            error = e
        }
        emitState()
    }

    fun onDappRequestHandled() {
        dAppRequestEntity = null
        emitState()
    }

    fun onErrorHandled() {
        error = null
        emitState()
    }

    fun disconnect(dapp: DAppEntity) {
        viewModelScope.launch(Dispatchers.Default) {
            tonConnectKit.disconnect(dapp)
        }
    }

}


data class TonConnectListUiState(
    val dapps: List<DAppEntity>,
    val dAppRequestEntity: DAppRequestEntity?,
    val error: Throwable?
)