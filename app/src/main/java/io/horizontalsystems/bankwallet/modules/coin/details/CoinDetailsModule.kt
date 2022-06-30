package io.horizontalsystems.bankwallet.modules.coin.details

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.marketkit.models.FullCoin

object CoinDetailsModule {

    class Factory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = CoinDetailsService(fullCoin, App.marketKit, App.currencyManager, App.proFeatureAuthorizationManager)

            return CoinDetailsViewModel(
                service
            ) as T
        }
    }

    @Immutable
    data class ViewItem(
        val proChartsActivated: Boolean,
        val tokenLiquidityViewItem: TokenLiquidityViewItem?,
        val tokenDistributionViewItem: TokenDistributionViewItem?,
        val volumeChart: ChartViewItem?,
        val tvlChart: ChartViewItem?,
        val tvlRank: String?,
        val tvlRatio: String?,
        val treasuries: String?,
        val fundsInvested: String?,
        val reportsCount: String?,
        val securityViewItems: List<SecurityViewItem>,
        val auditAddresses: List<String>
    )

    @Immutable
    data class ChartViewItem(
        val badge: String?,
        val value: String,
        val diff: String,
        val chartData: ChartData,
        val movementTrend: ChartMovementTrend
    )

    enum class ChartMovementTrend {
        Neutral,
        Down,
        Up,
    }

    @Immutable
    data class TokenLiquidityViewItem(
        val volume: ChartViewItem?,
        val liquidity: ChartViewItem?
    )

    @Immutable
    data class TokenDistributionViewItem(
        val txCount: ChartViewItem?,
        val txVolume: ChartViewItem?,
        val activeAddresses: ChartViewItem?,
        val hasMajorHolders: Boolean
    )

    @Immutable
    data class SecurityViewItem(
        val type: SecurityType,
        @StringRes
        val value: Int,
        val grade: SecurityGrade
    )

    enum class PrivacyLevel(
        @StringRes val title: Int,
        val grade: SecurityGrade,
    ) {
        High(
            R.string.CoinPage_SecurityParams_High,
            SecurityGrade.High,
        ),
        Medium(
            R.string.CoinPage_SecurityParams_Medium,
            SecurityGrade.Medium,
        ),
        Low(
            R.string.CoinPage_SecurityParams_Low,
            SecurityGrade.Low,
        ),
    }

    enum class IssuanceLevel(
        @StringRes val title: Int,
        val grade: SecurityGrade,
    ) {
        Decentralized(
            R.string.CoinPage_SecurityParams_Decentralized,
            SecurityGrade.High,
        ),
        Centralized(
            R.string.CoinPage_SecurityParams_Centralized,
            SecurityGrade.Low,
        )
    }

    enum class CensorshipResistanceLevel(
        @StringRes val title: Int,
        val grade: SecurityGrade,
    ) {
        Yes(
            R.string.CoinPage_SecurityParams_Yes,
            SecurityGrade.High,
        ),
        No(
            R.string.CoinPage_SecurityParams_No,
            SecurityGrade.Low,
        )
    }


    enum class ConfiscationResistanceLevel(
        @StringRes val title: Int,
        val grade: SecurityGrade,
    ) {
        Yes(
            R.string.CoinPage_SecurityParams_Yes,
            SecurityGrade.High,
        ),
        No(
            R.string.CoinPage_SecurityParams_No,
            SecurityGrade.Low,
        )
    }

    enum class SecurityGrade {
        Low, Medium, High;

        @Composable
        fun securityGradeColor() = when (this) {
            High -> ComposeAppTheme.colors.remus
            Medium -> ComposeAppTheme.colors.issykBlue
            Low -> ComposeAppTheme.colors.lucian
        }
    }

    enum class SecurityType(@StringRes val title: Int) {
        Privacy(R.string.CoinPage_SecurityParams_Privacy),
        Issuance(R.string.CoinPage_SecurityParams_Issuance),
        ConfiscationResistance(R.string.CoinPage_SecurityParams_ConfiscationResistance),
        CensorshipResistance(R.string.CoinPage_SecurityParams_CensorshipResistance)
    }
}
