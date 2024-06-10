package io.horizontalsystems.subscriptionskit

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface IPaidAction : Parcelable

@Parcelize
object EnableWatchlistSignals : IPaidAction
