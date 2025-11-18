package io.horizontalsystems.bankwallet.modules.usersubscription

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.subscriptions.core.AdvancedSearch
import io.horizontalsystems.subscriptions.core.BasePlan
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.LossProtection
import io.horizontalsystems.subscriptions.core.PricingPhase
import io.horizontalsystems.subscriptions.core.PrioritySupport
import io.horizontalsystems.subscriptions.core.RobberyProtection
import io.horizontalsystems.subscriptions.core.ScamProtection
import io.horizontalsystems.subscriptions.core.SecureSend
import io.horizontalsystems.subscriptions.core.TokenInsights
import io.horizontalsystems.subscriptions.core.TradeSignals
import java.time.Period

object BuySubscriptionModel {

    val IPaidAction.titleStringRes: Int
        get() = when (this) {
            TokenInsights -> R.string.Premium_UpgradeFeature_TokenInsights
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals
            RobberyProtection -> R.string.Premium_UpgradeFeature_RobberyProtection
            SecureSend -> R.string.Premium_UpgradeFeature_SecureSend
            ScamProtection -> R.string.Premium_UpgradeFeature_ScamProtection
            PrioritySupport -> R.string.Premium_UpgradeFeature_PrioritySupport
            LossProtection -> R.string.Premium_UpgradeFeature_LossProtection
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.descriptionStringRes: Int
        get() = when (this) {
            TokenInsights -> R.string.Premium_UpgradeFeature_TokenInsights_Description
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch_Description
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals_Description
            RobberyProtection -> R.string.Premium_UpgradeFeature_RobberyProtection_Description
            SecureSend -> R.string.Premium_UpgradeFeature_SecureSend_Description
            ScamProtection -> R.string.Premium_UpgradeFeature_ScamProtection_Description
            PrioritySupport -> R.string.Premium_UpgradeFeature_PrioritySupport_Description
            LossProtection -> R.string.Premium_UpgradeFeature_LossProtection_Description
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.iconRes: Int
        get() = when (this) {
            TokenInsights -> R.drawable.prem_binocular_24
            AdvancedSearch -> R.drawable.prem_search_24
            TradeSignals -> R.drawable.prem_bell_24
            RobberyProtection -> R.drawable.prem_fraud_24
            SecureSend -> R.drawable.prem_wallet_in_24
            ScamProtection -> R.drawable.prem_radar_24
            PrioritySupport -> R.drawable.prem_message_24
            LossProtection -> R.drawable.prem_usd_24
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

    fun BasePlan.stringRepresentation(): String {
        val phase = pricingPhases.last()
        return "${phase.formattedPrice} / ${phase.period()}"
    }

    fun BasePlan.badge(): String? {
        return when (pricingPhases.last().period.years) {
            1 -> Translator.getString(R.string.Premium_SubscriptionPeriod_AnnuallySave)
            else -> null
        }
    }

    //billing periods: P1M, P3M, P6M, P1Y
    private fun PricingPhase.period(): String {
        return when (billingPeriod) {
            "P1M" -> Translator.getString(R.string.Premium_SubscriptionPeriod_Month)
            "P1Y" -> Translator.getString(R.string.Premium_SubscriptionPeriod_Year)
            else -> billingPeriod
        }
    }
}
