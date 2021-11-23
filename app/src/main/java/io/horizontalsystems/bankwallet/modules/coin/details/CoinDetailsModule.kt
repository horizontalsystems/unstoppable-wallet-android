package io.horizontalsystems.bankwallet.modules.coin.details

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.coin.CoinViewFactory
import io.horizontalsystems.bankwallet.modules.market.Value
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.marketkit.models.FullCoin
import kotlinx.android.parcel.Parcelize

object CoinDetailsModule {

    class Factory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = CoinDetailsService(fullCoin, App.marketKit, App.currencyManager)

            return CoinDetailsViewModel(
                service,
                CoinViewFactory(App.currencyManager.baseCurrency, App.numberFormatter),
                App.numberFormatter
            ) as T
        }
    }

    @Immutable
    data class ViewItem(
        val hasMajorHolders: Boolean,
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
        val diff: Value,
        val chartData: ChartData
    )

    @Immutable
    data class SecurityViewItem(
        val type: SecurityType,
        @StringRes
        val value: Int,
        val grade: SecurityGrade
    )

    @Parcelize
    @Immutable
    data class SecurityInfoViewItem(
        val grade: SecurityGrade,
        @StringRes
        val title: Int,
        @StringRes
        val description: Int
    ) : Parcelable

    enum class PrivacyLevel(
        @StringRes val title: Int,
        val grade: SecurityGrade,
        @StringRes val description: Int
    ) {
        High(
            R.string.CoinPage_SecurityParams_High,
            SecurityGrade.High,
            R.string.CoinPage_SecurityParams_Privacy_High
        ),
        Medium(
            R.string.CoinPage_SecurityParams_Medium,
            SecurityGrade.Medium,
            R.string.CoinPage_SecurityParams_Privacy_Medium
        ),
        Low(
            R.string.CoinPage_SecurityParams_Low,
            SecurityGrade.Low,
            R.string.CoinPage_SecurityParams_Privacy_Low
        ),
    }

    enum class IssuanceLevel(
        @StringRes val title: Int,
        val grade: SecurityGrade,
        @StringRes val description: Int
    ) {
        Decentralized(
            R.string.CoinPage_SecurityParams_Decentralized,
            SecurityGrade.High,
            R.string.CoinPage_SecurityParams_Issuance_Decentralized
        ),
        Centralized(
            R.string.CoinPage_SecurityParams_Centralized,
            SecurityGrade.Low,
            R.string.CoinPage_SecurityParams_Issuance_Centralized
        )
    }

    enum class CensorshipResistanceLevel(
        @StringRes val title: Int,
        val grade: SecurityGrade,
        @StringRes val description: Int
    ) {
        Yes(
            R.string.CoinPage_SecurityParams_Yes,
            SecurityGrade.High,
            R.string.CoinPage_SecurityParams_CensorshipResistance_Yes
        ),
        No(
            R.string.CoinPage_SecurityParams_No,
            SecurityGrade.Low,
            R.string.CoinPage_SecurityParams_CensorshipResistance_No
        )
    }


    enum class ConfiscationResistanceLevel(
        @StringRes val title: Int,
        val grade: SecurityGrade,
        @StringRes val description: Int
    ) {
        Yes(
            R.string.CoinPage_SecurityParams_Yes,
            SecurityGrade.High,
            R.string.CoinPage_SecurityParams_ConfiscationResistance_Yes
        ),
        No(
            R.string.CoinPage_SecurityParams_No,
            SecurityGrade.Low,
            R.string.CoinPage_SecurityParams_ConfiscationResistance_No
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
