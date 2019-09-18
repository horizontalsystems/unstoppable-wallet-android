package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import kotlinx.android.synthetic.main.view_send_memo.*


class SendMemoFragment(val sendMemoViewModel: SendMemoViewModel) : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_send_memo, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        memoInput.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                sendMemoViewModel.delegate.onTextEntered(s?.toString() ?: "")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        sendMemoViewModel.maxLength.observe(viewLifecycleOwner, Observer { maxLength ->
            memoInput.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        })
    }
}
