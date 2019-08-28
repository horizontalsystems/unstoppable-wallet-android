package io.horizontalsystems.bankwallet.modules.settings

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_menu_item_big.view.*

class MenuItemBig : ConstraintLayout {

    private var attrTitle: String? = null
    private var attrSubtitle: String? = null
    private var attrIcon: Int? = null

    init {
        inflate(context, R.layout.view_menu_item_big, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        loadAttributes(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        loadAttributes(attrs)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        title.text = attrTitle
        subtitle.text = attrSubtitle

        attrIcon?.let { icon.setImageResource(it) }
    }

    private fun loadAttributes(attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.MenuItemBig, 0, 0)
        try {
            attrTitle = ta.getString(R.styleable.MenuItemBig_title)
            attrSubtitle = ta.getString(R.styleable.MenuItemBig_subtitle)
            attrIcon = ta.getResourceId(R.styleable.MenuItemBig_icon, 0)
        } finally {
            ta.recycle()
        }
    }
}
