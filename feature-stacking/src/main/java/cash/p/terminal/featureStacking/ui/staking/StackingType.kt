package cash.p.terminal.featureStacking.ui.staking

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class StackingType(val value: String) : Parcelable {
    PCASH("PIRATE"),
    COSANTA("COSA")
}