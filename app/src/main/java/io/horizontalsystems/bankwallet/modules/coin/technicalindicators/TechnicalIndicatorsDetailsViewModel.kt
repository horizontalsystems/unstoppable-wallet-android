package io.horizontalsystems.bankwallet.modules.coin.technicalindicators

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.technicalindicators.TechnicalIndicatorsDetailsModule.SectionViewItem
import io.horizontalsystems.bankwallet.modules.coin.technicalindicators.TechnicalIndicatorsDetailsModule.UiState
import io.horizontalsystems.marketkit.models.HsPointTimePeriod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TechnicalIndicatorsDetailsViewModel(
    private val service: TechnicalIndicatorService,
    private val factory: CoinIndicatorViewItemFactory,
    period: HsPointTimePeriod
) : ViewModel() {

    private var sections: List<SectionViewItem> = emptyList()
    private var techIndicatorPeriod: HsPointTimePeriod = period
    private var viewState: ViewState? = null

    var uiState by mutableStateOf(
        UiState(
            sections,
            viewState
        )
    )
        private set

    init {
        refresh()
    }

    fun refresh() {
        viewState = ViewState.Loading
        viewModelScope.launch {
            syncState()
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = service.fetch(techIndicatorPeriod)
            handle(result)
        }
    }

    private suspend fun handle(result: DataState<List<TechnicalIndicatorService.SectionItem>>?) {
        when (result) {
            is DataState.Success -> {
                sections = factory.detailViewItems(result.data)
                viewState = ViewState.Success
            }

            is DataState.Error -> {
                viewState = ViewState.Error(result.error)
            }

            DataState.Loading -> {
                viewState = ViewState.Loading
            }
            null -> {}
        }
        syncState()
    }

    private suspend fun syncState() {
        withContext(Dispatchers.Main) {
            uiState = UiState(
                sections,
                viewState
            )
        }
    }

}