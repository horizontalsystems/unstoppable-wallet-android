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
object RobberyProtection : IPaidAction

@Parcelize
object SecureSend : IPaidAction

@Parcelize
object ScamProtection : IPaidAction

@Parcelize
object LossProtection : IPaidAction

@Parcelize
object PrioritySupport : IPaidAction
