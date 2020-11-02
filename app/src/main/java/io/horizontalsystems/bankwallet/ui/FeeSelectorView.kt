package io.horizontalsystems.bankwallet.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ethereum.SendFeeSliderViewItem
import io.horizontalsystems.seekbar.FeeSeekBar
import kotlinx.android.synthetic.main.view_send_fee.view.*

class FeeSelectorView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    var onTxSpeedClickListener: OnClickListener? = null
        set(value) {
            field = value
            txSpeedLayout.setOnClickListener(field)
        }

    var customFeeSeekBarListener: FeeSeekBar.Listener? = null
        set(value) {
            field = value
            customFeeSeekBar.setListener(field)
        }

    init {
        inflate(context, R.layout.view_send_fee, this)
    }

    fun setFeeText(value: String) {
        txFeePrimary.text = value
        invalidate()
    }

    fun setDurationVisible(visible: Boolean) {
        txDurationLayout.isVisible = visible
        invalidate()
    }

    fun setPriorityText(value: String) {
        txSpeedMenu.text = value
        invalidate()
    }

    fun setFeeSliderViewItem(viewItem: SendFeeSliderViewItem?) {
        customFeeSeekBar.isVisible = viewItem != null

        viewItem?.let {
            customFeeSeekBar.min = it.range.lower.toInt()
            customFeeSeekBar.max = it.range.upper.toInt()
            customFeeSeekBar.progress = it.initialValue.toInt()
            customFeeSeekBar.setBubbleHint(it.unit)
        }

        invalidate()
    }

}