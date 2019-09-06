package io.horizontalsystems.bankwallet.ui.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.constraint_layout_with_header.view.*

open class ConstraintLayoutWithHeader : ConstraintLayout {

    init {
        inflate(context, R.layout.constraint_layout_with_header, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun setContentView(@LayoutRes resource: Int) {
        content.layoutResource = resource
        content.inflate()
    }

    fun setTitle(title: String?) {
        txtTitle.text = title
    }

    fun setSubtitle(subtitle: String?) {
        txtSubtitle.text = subtitle
    }

    fun setHeaderIcon(@DrawableRes resource: Int) {
        headerIcon.setImageResource(resource)
    }

    fun setOnCloseCallback(onCloseCallback: (() -> Unit)) {
        closeButton.setOnClickListener { onCloseCallback() }
    }

}
