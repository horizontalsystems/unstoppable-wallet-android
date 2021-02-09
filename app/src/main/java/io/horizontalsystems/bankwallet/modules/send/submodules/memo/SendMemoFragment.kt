package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment
import kotlinx.android.synthetic.main.view_send_memo.*


class SendMemoFragment(private val maxLength: Int,
                       private val handler: SendModule.ISendHandler) : SendSubmoduleFragment() {
    private val presenter by activityViewModels<SendMemoPresenter> { SendMemoModule.Factory(maxLength, handler) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.view_send_memo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val presenterView = presenter.view as SendMemoView

        memoInput.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                presenter.onTextEntered(s?.toString() ?: "")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        presenterView.maxLength.observe(viewLifecycleOwner, Observer { maxLength ->
            memoInput.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        })
    }

    override fun init() {
        presenter.onViewDidLoad()
    }
}
