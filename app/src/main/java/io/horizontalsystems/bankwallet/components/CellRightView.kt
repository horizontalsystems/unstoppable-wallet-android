package io.horizontalsystems.bankwallet.components

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_cell_right.view.*

class CellRightView : ConstraintLayout {

    init {
        inflate(context, R.layout.view_cell_right, this)
        enableIcon(null)
        cellTitle.visibility = View.GONE
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    var title: String? = null
        set(value) {
            field = value

            cellTitle.text = value
            cellTitle.visibility = if (value == null) View.GONE else View.VISIBLE
        }

    var dropdownText: String? = null
        set(value) {
            field = value

            dropdownValueText.text = value
            dropdownValueText.visibility = if (value == null) View.GONE else View.VISIBLE
        }

    var checked: Boolean = false
        set(value) {
            field = value
            enableIcon(if (value) checkIcon else null)
        }

    var dropDownArrow: Boolean = false
        set(value) {
            field = value
            enableIcon(if (value) dropDownIcon else null)
        }

    var rightArrow: Boolean = false
        set(value) {
            field = value
            enableIcon(if (value) arrowIcon else null)
        }

    var switchIsChecked: Boolean = false
        set(isChecked) {
            switchView.setOnCheckedChangeListener(null)
            switchView.isChecked = isChecked
            switchView.visibility = View.VISIBLE
            field = isChecked
            switchView.setOnCheckedChangeListener(switchOnCheckedChangeListener)
            invalidate()
        }

    var switchOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null
        set(value) {
            field = value
            switchView.setOnCheckedChangeListener(value)
            invalidate()
        }

    var badge: Boolean = false
        set(value) {
            field = value
            //badge can be shown along with other icon, its visibility shouldn't hide other icons
            badgeImage.visibility = if (value) View.VISIBLE else View.GONE
        }

    fun switchToggle() {
        switchView.toggle()
    }

    private fun enableIcon(icon: ImageView?) {
        listOf(lightModeIcon, dropDownIcon, arrowIcon, checkIcon).forEach {
            it.visibility = View.GONE
        }

        icon?.visibility = View.VISIBLE
    }
}
