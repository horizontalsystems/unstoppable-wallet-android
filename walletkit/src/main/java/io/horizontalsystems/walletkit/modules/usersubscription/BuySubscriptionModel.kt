package io.horizontalsystems.walletkit.modules.usersubscription

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.subscriptions.core.AdvancedSearch
import io.horizontalsystems.subscriptions.core.BasePlan
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.PricingPhase
import io.horizontalsystems.subscriptions.core.PrioritySupport
import io.horizontalsystems.subscriptions.core.ScamProtection
import io.horizontalsystems.subscriptions.core.SecureSend
import io.horizontalsystems.subscriptions.core.SwapProtection
import io.horizontalsystems.subscriptions.core.TradeSignals
import java.time.Period
import java.util.Locale
import kotlin.math.ceil

object BuySubscriptionModel {

    val IPaidAction.titleStringRes: Int
        get() = when (this) {
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals
            SecureSend -> R.string.Premium_UpgradeFeature_SecureSend
            ScamProtection -> R.string.Premium_UpgradeFeature_ScamProtection
            PrioritySupport -> R.string.Premium_UpgradeFeature_PrioritySupport
            SwapProtection -> R.string.Premium_UpgradeFeature_SwapProtection
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.descriptionStringRes: Int
        get() = when (this) {
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch_Description
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals_Description
            SecureSend -> R.string.Premium_UpgradeFeature_SecureSend_Description
            ScamProtection -> R.string.Premium_UpgradeFeature_ScamProtection_Description
            PrioritySupport -> R.string.Premium_UpgradeFeature_PrioritySupport_Description
            SwapProtection -> R.string.Premium_UpgradeFeature_SwapProtection_Description
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.iconRes: Int
        get() = when (this) {
            AdvancedSearch -> R.drawable.prem_search_24
            TradeSignals -> R.drawable.prem_bell_24
            PrioritySupport -> R.drawable.prem_message_24
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    fun BasePlan.title(): String {
        return pricingPhases.last().period.title()
    }

    fun Period.title(): String {
        return when {
            years > 0 -> Translator.getString(R.string.Premium_SubscriptionPeriod_Annually)
            months > 0 -> Translator.getString(R.string.Premium_SubscriptionPeriod_Monthly)
            else -> ""
        }
    }

    /** The plan's price as the store formats it for the user's country. */
    fun BasePlan.price(): String {
        return pricingPhases.last().formattedPrice
    }

    fun BasePlan.badge(): String? {
        return when {
            pricingPhases.last().period.years > 0 -> Translator.getString(R.string.Premium_SubscriptionPeriod_AnnuallySave)
            else -> null
        }
    }

    /** What a yearly plan comes to per month, as in "($16.67 / month)". */
    fun BasePlan.note(): String? {
        val phase = pricingPhases.last()
        return when {
            phase.period.years > 0 -> {
                val month = Translator.getString(R.string.Premium_SubscriptionPeriod_Month)
                "(${getYearlySaveAmount(phase)} / $month)"
            }
            else -> null
        }
    }

    fun getYearlySaveAmount(phase: PricingPhase): String {
        val currencySymbol = phase.formattedPrice.takeWhile { !it.isDigit() }
        val price = phase.priceAmountMicros / 1_000_000.0

        val pricePerMonth = price / 12

        // round up to 2 decimal places
        val rounded = ceil(pricePerMonth * 100) / 100.0

        val priceStr = String.format(Locale.US, "%.2f", rounded)

        return currencySymbol + priceStr
    }
}
