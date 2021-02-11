package io.horizontalsystems.bankwallet.modules.settings.terms

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.managers.Term
import io.horizontalsystems.bankwallet.core.managers.TermsManager
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_terms_settings.*

class TermsFragment : BaseFragment() {

    private val viewModel by viewModels<TermsViewModel> { TermsModule.Factory() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_terms_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        viewModel.termsLiveData.observe(viewLifecycleOwner, Observer { terms ->
            setCheckbox(checkboxAcademy, TermsManager.termIds[0], terms)
            setCheckbox(checkboxBackup, TermsManager.termIds[1], terms)
            setCheckbox(checkboxOwner, TermsManager.termIds[2], terms)
            setCheckbox(checkboxRecover, TermsManager.termIds[3], terms)
            setCheckbox(checkboxPhone, TermsManager.termIds[4], terms)
            setCheckbox(checkboxRoot, TermsManager.termIds[5], terms)
            setCheckbox(checkboxBugs, TermsManager.termIds[6], terms)
            setCheckbox(checkboxPin, TermsManager.termIds[7], terms)
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
