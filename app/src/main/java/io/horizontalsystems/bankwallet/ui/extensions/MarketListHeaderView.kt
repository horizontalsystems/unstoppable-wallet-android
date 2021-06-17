package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.widget.RadioButton
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.market.SortingField
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_market_list_header.view.*

class MarketListHeaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    interface Listener {
        fun onClickSortingField()
        fun onSelectFieldViewOption(fieldViewOptionId: Int)
    }

    var listener: Listener? = null

    init {
        inflate(context, R.layout.view_market_list_header, this)

        sortingField.setOnSingleClickListener {
            listener?.onClickSortingField()
        }
    }

    fun setFieldViewOptions(items: List<FieldViewOption>){
        val buttonParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(LayoutHelper.dp(8f, context), 0, 0, 0)
        }

        marketFieldSelector.removeAllViews()

        items.forEach { item ->
            val button = RadioButton(context, null, 0, R.style.RadioButtonThird).apply {
                id = item.index
                text = item.title
                isChecked = item.selected
                isClickable = true
                layoutParams = buttonParams
            }

            marketFieldSelector.addView(button)
        }

        marketFieldSelector.setOnCheckedChangeListener { _, checkedId ->
            listener?.onSelectFieldViewOption(checkedId)
        }

    }

    fun setSortingField(fieldToSort: SortingField) {
        val sortingFieldText = context.getString(fieldToSort.titleResId)
        if (sortingField.text != sortingFieldText) {
            sortingField.text = sortingFieldText
        }
    }

    data class FieldViewOption(val index: Int, val title: String, var selected: Boolean)
}
