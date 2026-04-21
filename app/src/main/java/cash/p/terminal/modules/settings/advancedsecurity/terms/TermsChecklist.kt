package cash.p.terminal.modules.settings.advancedsecurity.terms

import cash.p.terminal.ui_compose.entities.TermItem

internal class TermsChecklist(termTitles: Array<String>) {
    private var terms = termTitles.mapIndexed { index, title ->
        TermItem(
            id = index,
            title = title,
            checked = false
        )
    }.toMutableList()

    fun state(): TermsChecklistState {
        return TermsChecklistState(
            terms = terms.toList(),
            agreeEnabled = terms.all { it.checked }
        )
    }

    fun toggle(id: Int) {
        val index = terms.indexOfFirst { it.id == id }
        if (index == -1) return

        val term = terms[index]
        terms[index] = term.copy(checked = !term.checked)
    }
}

internal data class TermsChecklistState(
    val terms: List<TermItem>,
    val agreeEnabled: Boolean
)
