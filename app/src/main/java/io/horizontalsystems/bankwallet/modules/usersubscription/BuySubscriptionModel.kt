package io.horizontalsystems.bankwallet.modules.usersubscription

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.subscriptions.core.AddressBlacklist
import io.horizontalsystems.subscriptions.core.AddressPhishing
import io.horizontalsystems.subscriptions.core.AdvancedSearch
import io.horizontalsystems.subscriptions.core.BasePlan
import io.horizontalsystems.subscriptions.core.DuressMode
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.PricingPhase
import io.horizontalsystems.subscriptions.core.TokenInsights
import io.horizontalsystems.subscriptions.core.TradeSignals
import io.horizontalsystems.subscriptions.core.VIPSupport
import java.time.Period

object BuySubscriptionModel {

    val IPaidAction.titleStringRes: Int
        get() = when (this) {
            TokenInsights -> R.string.Premium_UpgradeFeature_TokenInsights
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals
            DuressMode -> R.string.Premium_UpgradeFeature_DuressMode
            AddressPhishing -> R.string.Premium_UpgradeFeature_AddressPhishing
            AddressBlacklist -> R.string.Premium_UpgradeFeature_AddressBlacklist
            VIPSupport -> R.string.Premium_UpgradeFeature_VipSupport
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.descriptionStringRes: Int
        get() = when (this) {
            TokenInsights -> R.string.Premium_UpgradeFeature_TokenInsights_Description
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch_Description
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals_Description
            DuressMode -> R.string.Premium_UpgradeFeature_DuressMode_Description
            AddressPhishing -> R.string.Premium_UpgradeFeature_AddressPhishing_Description
            AddressBlacklist -> R.string.Premium_UpgradeFeature_AddressBlacklist_Description
            VIPSupport -> R.string.Premium_UpgradeFeature_VipSupport_Description
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.bigDescriptionStringRes: Int
        get() = when (this) {
            TokenInsights -> R.string.Premium_UpgradeFeature_TokenInsights_BigDescription
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch_BigDescription
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals_BigDescription
            DuressMode -> R.string.Premium_UpgradeFeature_DuressMode_BigDescription
            AddressPhishing -> R.string.Premium_UpgradeFeature_AddressPhishing_BigDescription
            AddressBlacklist -> R.string.Premium_UpgradeFeature_AddressBlacklist_BigDescription
            VIPSupport -> R.string.Premium_UpgradeFeature_VipSupport_BigDescription
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.iconRes: Int
        get() = when (this) {
            TokenInsights -> R.drawable.prem_portfolio_24
            AdvancedSearch -> R.drawable.prem_search_discovery_24
            TradeSignals -> R.drawable.prem_ring_24
            DuressMode -> R.drawable.prem_duress_24
            AddressPhishing -> R.drawable.prem_shield_24
            AddressBlacklist -> R.drawable.prem_warning_24
            VIPSupport -> R.drawable.prem_vip_support_24
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
