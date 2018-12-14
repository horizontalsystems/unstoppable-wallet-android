package io.horizontalsystems.bankwallet.ui.view

import android.view.View


abstract class SingleClickListener : View.OnClickListener {

    override fun onClick(v: View) {
        if (SingleClickManager.canBeClicked()) {
            onSingleClick(v)
        }
    }

    abstract fun onSingleClick(v: View)

}
