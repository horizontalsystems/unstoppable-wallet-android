package com.quantum.wallet.bankwallet.modules.market.earn.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.chart.ChartModule
import com.quantum.wallet.bankwallet.modules.chart.ChartViewModel

class VaultViewModel(
    input: VaultFragment.Input,
) : ViewModelUiState<VaultModule.UiState>() {

    private var viewState: ViewState = ViewState.Loading
    private var vaultViewItem: VaultModule.VaultViewItem = VaultModule.VaultViewItem(
        rank = "#" + input.rank.toString(),
        address = input.address,
        name = input.name,
        tvl = input.tvl,
        chain = input.chain,
        url = input.url,
        holders = input.holders,
        assetSymbol = input.assetSymbol,
        protocolName = input.protocolName,
        assetLogo = input.assetLogo
    )
    private var isRefreshing = false

    override fun createState(): VaultModule.UiState {
        return VaultModule.UiState(
            isRefreshing = isRefreshing,
            viewState = viewState,
            vaultViewItem = vaultViewItem
        )
    }
}

object VaultModule {

    class Factory(private val input: VaultFragment.Input) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                VaultViewModel::class.java -> {
                    VaultViewModel(input) as T
                }

                ChartViewModel::class.java -> {
                    val chartService =
                        VaultChartService(input.address, App.currencyManager, App.marketKit)
                    val chartNumberFormatter = VaultChartFormatter()
                    ChartModule.createViewModel(chartService, chartNumberFormatter) as T
                }

                else -> throw IllegalArgumentException()
            }
        }
    }

    data class VaultViewItem(
        val rank: String,
        val address: String,
        val name: String,
        val tvl: String,
        val chain: String,
        val url: String?,
        val holders: String?,
        val assetSymbol: String,
        val protocolName: String,
        val assetLogo: String?,
    )

    data class UiState(
        val isRefreshing: Boolean,
        val viewState: ViewState,
        val vaultViewItem: VaultViewItem,
    )
}