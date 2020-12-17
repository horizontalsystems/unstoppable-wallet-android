package io.horizontalsystems.bankwallet.modules.market.top

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R

class MarketTopViewModel : ViewModel() {
    val sortingFields: Array<Field> = Field.values()
    val sortingPeriods: Array<Period> = Period.values()

    var sortingField: Field = Field.HighestCap
        set(value) {
            field = value

            sortingFieldLiveData.postValue(value)
        }
    var sortingFieldLiveData = MutableLiveData(sortingField)

    var sortingPeriod: Period = Period.Period24h
        set(value) {
            field = value

            sortingPeriodLiveData.postValue(value)
        }
    var sortingPeriodLiveData = MutableLiveData(sortingPeriod)

    enum class Field(@StringRes val titleResId: Int) {
        HighestCap(R.string.Market_Sort_HighestCap), LowestCap(R.string.Market_Sort_LowestCap),
        HighestVolume(R.string.Market_Sort_HighestVolume), LowestVolume(R.string.Market_Sort_LowestVolume),
    }

    enum class Period(@StringRes val titleResId: Int) {
        Period24h(R.string.Market_Period_24h),
        PeriodWeek(R.string.Market_Period_1week),
        PeriodMonth(R.string.Market_Period_1month)
    }

}
