package cash.p.terminal.modules.settings.advancedsecurity.terms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.R
import cash.p.terminal.ui_compose.AnnotatedResourceString
import cash.p.terminal.ui_compose.components.TextImportantWarning
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui_compose.entities.TermItem
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

@Composable
internal fun DeleteContactsTermsScreen(
    uiState: DeleteContactsTermsUiState,
    onCheckboxToggle: (Int) -> Unit,
    onAgreeClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val agreementHtml = stringResource(R.string.delete_all_contacts_terms_agreement)
    val agreement = remember(agreementHtml) {
        parseDeleteContactsAgreement(agreementHtml)
    }

    ChecklistTermsScreen(
        title = stringResource(R.string.AdvancedSecurity_Terms_Title),
        terms = uiState.terms,
        buttonTitle = stringResource(R.string.Button_IAgree),
        buttonEnabled = uiState.agreeEnabled,
        onCheckboxToggle = onCheckboxToggle,
        onAgreeClick = onAgreeClick,
        onNavigateBack = onNavigateBack,
        warningContent = {
            if (agreement == null) {
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = AnnotatedResourceString.htmlToAnnotatedString(agreementHtml)
                )
            } else {
                TextImportantWarning(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    DeleteContactsAgreementContent(agreement)
                }
            }
        }
    )
}

@Composable
private fun DeleteContactsAgreementContent(agreement: DeleteContactsAgreement) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        subhead2_leah(text = parseAgreementSegment(agreement.title))
        subhead2_leah(text = parseAgreementSegment(agreement.intro))

        agreement.sections.forEach { section ->
            DeleteContactsAgreementSection(section)
        }
    }
}

@Composable
private fun DeleteContactsAgreementSection(section: DeleteContactsAgreementSection) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        subhead2_leah(
            text = section.number,
            modifier = Modifier.width(20.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            subhead2_leah(text = parseBoldAgreementSegment(section.title))
            subhead2_leah(text = parseAgreementSegment(section.description))
        }
    }
}

internal fun parseDeleteContactsAgreement(html: String): DeleteContactsAgreement? {
    val titleSeparatorMatch = TITLE_SEPARATOR_REGEX.find(html) ?: return null
    val titleHtml = html.substring(0, titleSeparatorMatch.range.first)
    val afterTitle = html.substring(titleSeparatorMatch.range.last + 1)
    val firstSectionMatch = SECTION_REGEX.find(afterTitle) ?: return null
    val introHtml = afterTitle
        .substring(0, firstSectionMatch.range.first)
        .removeSuffixBreak()

    val sections = SECTION_REGEX.findAll(afterTitle).map { match ->
        DeleteContactsAgreementSection(
            number = "${match.groupValues[1]}.",
            title = match.groupValues[2].trim(),
            description = match.groupValues[3].removeSuffixBreak()
        )
    }.toList()

    if (sections.isEmpty()) return null

    return DeleteContactsAgreement(
        title = titleHtml.trim(),
        intro = introHtml.trim(),
        sections = sections
    )
}

private fun String.removeSuffixBreak(): String {
    return replace(SINGLE_BREAK_REGEX, "").trim()
}

private fun parseAgreementSegment(html: String): AnnotatedString {
    return AnnotatedResourceString.htmlToAnnotatedString(html.trim())
}

private fun parseBoldAgreementSegment(text: String): AnnotatedString {
    return parseAgreementSegment("<b>${text.trim()}</b>")
}

internal class DeleteContactsAgreement(
    val title: String,
    val intro: String,
    val sections: List<DeleteContactsAgreementSection>
)

internal class DeleteContactsAgreementSection(
    val number: String,
    val title: String,
    val description: String
)

private val TITLE_SEPARATOR_REGEX = Regex("""<br\s*/?>\s*<br\s*/?>""")
private val SINGLE_BREAK_REGEX = Regex("""<br\s*/?>\s*$""")
private val SECTION_REGEX = Regex(
    pattern = """<b>\s*(\d+)\.\s*(.*?)\s*</b>\s*<br\s*/?>\s*(.*?)(?=(?:<br\s*/?>\s*<b>\s*\d+\.)|$)""",
    options = setOf(RegexOption.DOT_MATCHES_ALL)
)

@Preview(showBackground = true)
@Composable
private fun DeleteContactsTermsScreenPreview() {
    val termTitles = stringArrayResource(R.array.delete_all_contacts_terms_checkboxes)
    val terms = termTitles.mapIndexed { index, title ->
        TermItem(
            id = index,
            title = title,
            checked = index == 0
        )
    }

    ComposeAppTheme {
        DeleteContactsTermsScreen(
            uiState = DeleteContactsTermsUiState(
                terms = terms,
                agreeEnabled = false
            ),
            onCheckboxToggle = {},
            onAgreeClick = {},
            onNavigateBack = {}
        )
    }
}
