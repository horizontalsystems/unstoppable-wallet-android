package io.horizontalsystems.bankwallet.modules.tonconnect

import androidx.lifecycle.viewModelScope
import com.tonapps.wallet.data.tonconnect.entities.DAppEntity
import com.tonapps.wallet.data.tonconnect.entities.DAppRequestEntity
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import kotlinx.coroutines.launch

class TonConnectListViewModel : ViewModelUiState<TonConnectListUiState>() {

    private val tonConnectKit = App.tonConnectKit

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

}


data class TonConnectListUiState(
    val dapps: List<DAppEntity>,
    val dAppRequestEntity: DAppRequestEntity?,
    val error: Throwable?
)