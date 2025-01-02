package io.horizontalsystems.subscriptions.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface IPaidAction : Parcelable

@Parcelize
object TokenInsights : IPaidAction

@Parcelize
object AdvancedSearch : IPaidAction

@Parcelize
object TradeSignals : IPaidAction

@Parcelize
object TransactionSpeedTools : IPaidAction

@Parcelize
object DuressMode : IPaidAction

@Parcelize
object AddressVerification : IPaidAction

@Parcelize
object Tor : IPaidAction

@Parcelize
object PrivacyMode : IPaidAction

@Parcelize
object VIPSupport : IPaidAction

@Parcelize
object VIPClub : IPaidAction
