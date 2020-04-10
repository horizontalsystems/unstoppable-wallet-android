package io.horizontalsystems.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.views.helpers.LayoutHelper
import kotlinx.android.synthetic.main.view_cell.view.*

class CellView : ConstraintLayout {

    private val singleLineHeight = 44f
    private val doubleLineHeight = 60f

    var coinIcon: String? = null
        set(value) {
            field = value
            field?.let { cellIcon.bind(it) }
            cellIcon.showIf(value != null)
        }

    var imageDrawable: Drawable? = null
        set(value) {
            field = value
            field?.let { cellIcon.bind(it) }
            cellIcon.showIf(value != null)
        }

    var imageTint: ColorStateList? = null
        set(value) {
            field = value
            field?.let { cellIcon.setTint(it) }
        }

    var title: String? = null
        set(value) {
            field = value
            cellTitle.text = value
        }

    var subtitle: String? = null
        set(value) {
            field = value
            cellSubtitle.text = value
            cellSubtitle.showIf(value != null)
            layoutParams?.height = LayoutHelper.dp(if (value == null) singleLineHeight else doubleLineHeight, context)
        }

    var rightTitle: String? = null
        set(value) {
            field = value
            cellLabel.text = value
            cellLabel.showIf(value != null)
        }

    var dropDownText: String? = null
        set(value) {
            field = value
            dropdownValue.text = value
            dropdownValue.showIf(value != null)
        }

    var checked: Boolean = false
        set(value) {
            field = value
            enableIcon(if (value) checkIcon else null)
        }

    var bottomBorder: Boolean = false
        set(value) {
            field = value
            bottomShade.showIf(value, View.INVISIBLE)
        }

    var dropDownArrow: Boolean = false
        set(value) {
            field = value
            enableIcon(if (value) dropDownIcon else null)
        }

    var badge: Boolean = false
        set(value) {
            field = value
            badgeImage.showIf(value)
        }

    var rightArrow: Boolean = false
        set(value) {
            field = value
            enableIcon(if (value) arrowIcon else null)
        }

    var switchIsChecked: Boolean? = null
        set(value) {
            field = value
            field?.let { isChecked ->
                switchView.setOnCheckedChangeListener(null)
                switchView.isChecked = isChecked
                switchView.visibility = View.VISIBLE
                switchView.setOnCheckedChangeListener(switchOnCheckedChangeListener)
            }
        }

    var switchOnCheckedChangeListener: CompoundButton.OnCheckedChangeListener? = null
        set(value) {
            field = value
            switchView.setOnCheckedChangeListener(value)
        }

    init {
        inflate(context, R.layout.view_cell, this)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialize(attrs)
    }

    fun switchToggle() {
        switchView.toggle()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!subtitle.isNullOrBlank()) {
            layoutParams.height = LayoutHelper.dp(doubleLineHeight, context)
        }
    }

    private fun initialize(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.CellView)
        try {
            title = ta.getString(R.styleable.CellView_title)
            subtitle = ta.getString(R.styleable.CellView_subtitle)
            rightTitle = ta.getString(R.styleable.CellView_rightTitle)
            imageDrawable = ta.getDrawable(R.styleable.CellView_imageDrawable)
            imageTint = ta.getColorStateList(R.styleable.CellView_imageTint)
            rightArrow = ta.getBoolean(R.styleable.CellView_rightArrow, false)
            bottomBorder = ta.getBoolean(R.styleable.CellView_bottomBorder, false)
        } finally {
            ta.recycle()
        }
    }

    private fun enableIcon(icon: ImageView?) {
        listOf(lightModeIcon, dropDownIcon, arrowIcon, checkIcon).forEach {
            it.visibility = View.GONE
        }

        icon?.visibility = View.VISIBLE
    }
}
