package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.view_send_memo.view.*


class SendMemoView : ConstraintLayout {

    init {
        inflate(context, io.horizontalsystems.bankwallet.R.layout.view_send_memo, this)
    }

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, lifecycleOwner: LifecycleOwner, sendMemoViewModel: SendMemoViewModel) : super(context) {

        memoInput.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sendMemoViewModel.delegate.onTextEntered(s?.toString() ?: "")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        sendMemoViewModel.maxLength.observe(lifecycleOwner, Observer { maxLength ->
            memoInput.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        })

    }

}
