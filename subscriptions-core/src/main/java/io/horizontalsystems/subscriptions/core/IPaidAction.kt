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
object DuressMode : IPaidAction

@Parcelize
object AddressPhishing : IPaidAction

@Parcelize
object AddressBlacklist : IPaidAction

@Parcelize
object VIPSupport : IPaidAction
