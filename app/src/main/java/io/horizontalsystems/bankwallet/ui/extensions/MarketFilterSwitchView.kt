package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.CompoundButton
import android.widget.LinearLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_market_filter_switch.view.*

class MarketFilterSwitchView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : LinearLayout(context, attrs, defStyleAttr), CompoundButton.OnCheckedChangeListener {

    private var onCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null
    private var notifyListener = true

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        inflate(context, R.layout.view_market_filter_switch, this)

        switchView.setOnCheckedChangeListener(this)

        setOnClickListener { switchToggle() }

        val ta = context.obtainStyledAttributes(attrs, R.styleable.MarketFilterSwitchView)
        try {
            title.text = ta.getString(R.styleable.MarketFilterSwitchView_title)
            setChecked(ta.getBoolean(R.styleable.MarketFilterSwitchView_checked, false))
        } finally {
            ta.recycle()
        }
    }

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (notifyListener) {
            onCheckedChangeListener?.onCheckedChanged(buttonView, isChecked)
        }
    }

    fun onCheckedChange(listener: (Boolean) -> Unit) {
        onCheckedChangeListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            listener(isChecked)
        }
    }

    fun setChecked(checked: Boolean) {
        notifyListener = false
        switchView.isChecked = checked
        notifyListener = true
    }

    private fun switchToggle() {
        switchView.toggle()
    }
}
