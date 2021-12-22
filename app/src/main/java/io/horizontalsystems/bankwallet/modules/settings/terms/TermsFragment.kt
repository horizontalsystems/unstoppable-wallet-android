package io.horizontalsystems.bankwallet.modules.settings.terms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.managers.Term
import io.horizontalsystems.bankwallet.core.managers.TermsManager
import io.horizontalsystems.bankwallet.databinding.FragmentTermsSettingsBinding
import io.horizontalsystems.core.findNavController

class TermsFragment : BaseFragment() {

    private val viewModel by viewModels<TermsViewModel> { TermsModule.Factory() }

    private var _binding: FragmentTermsSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTermsSettingsBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.termsLiveData.observe(viewLifecycleOwner, Observer { terms ->
            setCheckbox(binding.checkboxAcademy, TermsManager.termIds[0], terms)
            setCheckbox(binding.checkboxBackup, TermsManager.termIds[1], terms)
            setCheckbox(binding.checkboxOwner, TermsManager.termIds[2], terms)
            setCheckbox(binding.checkboxRecover, TermsManager.termIds[3], terms)
            setCheckbox(binding.checkboxPhone, TermsManager.termIds[4], terms)
            setCheckbox(binding.checkboxRoot, TermsManager.termIds[5], terms)
            setCheckbox(binding.checkboxBugs, TermsManager.termIds[6], terms)
            setCheckbox(binding.checkboxPin, TermsManager.termIds[7], terms)
        })
    }

    private fun setCheckbox(chechkbox: CheckBox, termKey: String, terms: List<Term>) {
        val index = terms.indexOfFirst { it.id == termKey }
        if (index < 0) {
            throw Exception("No such item in terms")
        }
        chechkbox.isChecked = terms[index].checked
        chechkbox.setOnCheckedChangeListener { _, _ ->
            viewModel.onTapTerm(index)
        }
    }
}
