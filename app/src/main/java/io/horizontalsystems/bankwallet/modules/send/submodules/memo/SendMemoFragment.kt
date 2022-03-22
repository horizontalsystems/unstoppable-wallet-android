package io.horizontalsystems.bankwallet.modules.send.submodules.memo

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.databinding.ViewSendMemoBinding
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.bankwallet.modules.send.submodules.SendSubmoduleFragment

class SendMemoFragment(
    private val maxLength: Int, hidden: Boolean,
    private val handler: SendModule.ISendHandler
) : SendSubmoduleFragment() {
    private val presenter by activityViewModels<SendMemoPresenter> {
        SendMemoModule.Factory(maxLength, hidden, handler)
    }

    private var _binding: ViewSendMemoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewSendMemoBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val presenterView = presenter.view as SendMemoView

        binding.memoInput.addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                presenter.onTextEntered(s?.toString() ?: "")
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        presenterView.maxLength.observe(viewLifecycleOwner, Observer { maxLength ->
            binding.memoInput.filters = arrayOf(InputFilter.LengthFilter(maxLength))
        })

        presenterView.hidden.observe(viewLifecycleOwner, { hidden ->
            binding.memoWrapper.isVisible = !hidden
        })
    }

    override fun init() {
        presenter.onViewDidLoad()
    }
}
