package io.horizontalsystems.bankwallet.modules.usersubscription

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.subscriptions.core.AddressVerification
import io.horizontalsystems.subscriptions.core.AdvancedSearch
import io.horizontalsystems.subscriptions.core.BasePlan
import io.horizontalsystems.subscriptions.core.DuressMode
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.PricingPhase
import io.horizontalsystems.subscriptions.core.PrivacyMode
import io.horizontalsystems.subscriptions.core.TokenInsights
import io.horizontalsystems.subscriptions.core.Tor
import io.horizontalsystems.subscriptions.core.TradeSignals
import io.horizontalsystems.subscriptions.core.TransactionSpeedTools
import io.horizontalsystems.subscriptions.core.VIPClub
import io.horizontalsystems.subscriptions.core.VIPSupport

object BuySubscriptionModel {

    val IPaidAction.titleStringRes: Int
        get() = when (this) {
            TokenInsights -> R.string.Premium_UpgradeFeature_TokenInsights
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals
            TransactionSpeedTools -> R.string.Premium_UpgradeFeature_TxSpeedTools
            DuressMode -> R.string.Premium_UpgradeFeature_DuressMode
            AddressVerification -> R.string.Premium_UpgradeFeature_AddressVerification
            Tor -> R.string.Premium_UpgradeFeature_Tor
            PrivacyMode -> R.string.Premium_UpgradeFeature_PrivacyMode
            VIPSupport -> R.string.Premium_UpgradeFeature_VipSupport
            VIPClub -> R.string.Premium_UpgradeFeature_VipClub
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.descriptionStringRes: Int
        get() = when (this) {
            TokenInsights -> R.string.Premium_UpgradeFeature_TokenInsights_Description
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch_Description
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals_Description
            TransactionSpeedTools -> R.string.Premium_UpgradeFeature_TxSpeedTools_Description
            DuressMode -> R.string.Premium_UpgradeFeature_DuressMode_Description
            AddressVerification -> R.string.Premium_UpgradeFeature_AddressVerification_Description
            Tor -> R.string.Premium_UpgradeFeature_Tor_Description
            PrivacyMode -> R.string.Premium_UpgradeFeature_PrivacyMode_Description
            VIPSupport -> R.string.Premium_UpgradeFeature_VipSupport_Description
            VIPClub -> R.string.Premium_UpgradeFeature_VipClub_Description
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.bigDescriptionStringRes: Int
        get() = when (this) {
            TokenInsights -> R.string.Premium_UpgradeFeature_TokenInsights_BigDescription
            AdvancedSearch -> R.string.Premium_UpgradeFeature_AdvancedSearch_BigDescription
            TradeSignals -> R.string.Premium_UpgradeFeature_TradeSignals_BigDescription
            TransactionSpeedTools -> R.string.Premium_UpgradeFeature_TxSpeedTools_BigDescription
            DuressMode -> R.string.Premium_UpgradeFeature_DuressMode_BigDescription
            AddressVerification -> R.string.Premium_UpgradeFeature_AddressVerification_BigDescription
            Tor -> R.string.Premium_UpgradeFeature_Tor_BigDescription
            PrivacyMode -> R.string.Premium_UpgradeFeature_PrivacyMode_BigDescription
            VIPSupport -> R.string.Premium_UpgradeFeature_VipSupport_BigDescription
            VIPClub -> R.string.Premium_UpgradeFeature_VipClub_BigDescription
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    val IPaidAction.iconRes: Int
        get() = when (this) {
            TokenInsights -> R.drawable.prem_portfolio_24
            AdvancedSearch -> R.drawable.prem_search_discovery_24
            TradeSignals -> R.drawable.prem_ring_24
            TransactionSpeedTools -> R.drawable.prem_outgoingraw_24
            DuressMode -> R.drawable.prem_duress_24
            AddressVerification -> R.drawable.prem_shield_24
            Tor -> R.drawable.prem_tor_24
            PrivacyMode -> R.drawable.prem_fraud_24
            VIPSupport -> R.drawable.prem_vip_support_24
            VIPClub -> R.drawable.prem_chat_support_24
            else -> throw IllegalArgumentException("Unknown IPaidAction")
        }

    fun BasePlan.stringRepresentation(): String {
        return pricingPhases.map {
            "${it.formattedPrice} / ${it.period()}"
        }.joinToString(" then ")
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
