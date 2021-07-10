package io.horizontalsystems.bankwallet.modules.send.submodules.sendbutton

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import androidx.constraintlayout.widget.ConstraintLayout
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener

class ProceedButtonView : ConstraintLayout {

    private var btnProceed: Button

    init {
        val rootView = inflate(context, R.layout.view_button_proceed, this)
        btnProceed = rootView.findViewById(R.id.btnProceed)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun bind(onClick: (() -> (Unit))? = null) {
        btnProceed.isEnabled = false
        btnProceed.setOnSingleClickListener { onClick?.invoke() }
    }

    fun updateState(enabled: Boolean) {
        btnProceed.isEnabled = enabled
    }

    fun setTitle(title: String) {
        btnProceed.text = title
    }
}
