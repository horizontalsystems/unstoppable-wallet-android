package io.horizontalsystems.bankwallet.modules.settings.terms

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.managers.Term
import io.horizontalsystems.bankwallet.core.managers.TermsManager
import kotlinx.android.synthetic.main.activity_terms_settings.*

class TermsActivity : BaseActivity() {

    private val viewModel by viewModels<TermsViewModel> { TermsModule.Factory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_settings)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        observeLiveData()
    }

    private fun observeLiveData() {
        viewModel.termsLiveData.observe(this, Observer { terms ->
            setCheckbox(checkboxAcademy, TermsManager.termIds[0], terms)
            setCheckbox(checkboxBackup, TermsManager.termIds[1], terms)
            setCheckbox(checkboxOwner, TermsManager.termIds[2], terms)
            setCheckbox(checkboxRecover, TermsManager.termIds[3], terms)
            setCheckbox(checkboxPhone, TermsManager.termIds[4], terms)
            setCheckbox(checkboxRoot, TermsManager.termIds[5], terms)
            setCheckbox(checkboxBugs, TermsManager.termIds[6], terms)
        })
    }

    private fun setCheckbox(chechkbox: CheckBox, termKey: String, terms: List<Term>) {
        val index = terms.indexOfFirst { it.id == termKey }
        if (index < 0){
            throw Exception("No such item in terms")
        }
        chechkbox.isChecked = terms[index].checked
        chechkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            viewModel.onTapTerm(index)
        }
    }

    companion object {
        fun start(context: Activity) {
            val intent = Intent(context, TermsActivity::class.java)
            context.startActivity(intent)
        }
    }
}
