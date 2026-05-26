package io.horizontalsystems.bankwallet.modules.metricchart

import io.horizontalsystems.bankwallet.R

object ProChartModule {

    enum class ChartType(val titleRes: Int) {
        CexVolume(R.string.CoinAnalytics_CexVolume),
        DexVolume(R.string.CoinAnalytics_DexVolume),
        DexLiquidity(R.string.CoinAnalytics_DexLiquidity),
        TxCount(R.string.CoinAnalytics_TransactionCount),
        AddressesCount(R.string.CoinAnalytics_ActiveAddresses),
        Tvl(R.string.CoinAnalytics_ProjectTvl)
    }

}
