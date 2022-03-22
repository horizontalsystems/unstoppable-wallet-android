package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITermsManager
import io.horizontalsystems.bankwallet.modules.settings.terms.TermsModule
import io.reactivex.subjects.PublishSubject

class TermsManager(private val localStorage: ILocalStorage) : ITermsManager {

    override val termsAcceptedSignal = PublishSubject.create<Boolean>()

    override val terms: List<Term>
        get() = TermsModule.TermType.values().map { term(it.key) }

    override val termsAccepted: Boolean
        get() = terms.all { it.checked }

    override fun update(term: Term) {
        val oldTermsAccepted = termsAccepted
        val currentTerms = terms
        currentTerms.firstOrNull { it.id == term.id }?.checked = term.checked
        localStorage.checkedTerms = currentTerms
        val newTermsAccepted = termsAccepted

        if (oldTermsAccepted != newTermsAccepted) {
            termsAcceptedSignal.onNext(newTermsAccepted)
        }
    }

    private fun term(id: String): Term {
        return Term(id, localStorage.checkedTerms.firstOrNull { it.id == id }?.checked ?: false)
    }
}

data class Term(val id: String, var checked: Boolean)
