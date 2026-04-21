package cash.p.terminal.modules.settings.advancedsecurity.terms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeleteContactsTermsScreenTest {

    @Test
    fun parseDeleteContactsAgreement_validAgreement_parsesStructuredSections() {
        val agreement = parseDeleteContactsAgreement(
            """
            Пользовательское соглашение по установке PIN-кода для удаления адресной книги крипто-адресов.<br/><br/>
            Устанавливая PIN-код, вы подтверждаете, что понимаете его назначение как механизма защиты вашей конфиденциальности.<br/>
            <b>1. Защитная функция PIN-кода</b><br/>
            Данный PIN-код предназначен для экстренного удаления адресной книги крипто-адресов.<br/>
            <b>2. Механизм действия</b><br/>
            Ввод установленного PIN-кода приведёт к немедленному удалению адресной книги.
            """.trimIndent()
        )

        assertNotNull(agreement)
        requireNotNull(agreement)

        assertEquals(
            "Пользовательское соглашение по установке PIN-кода для удаления адресной книги крипто-адресов.",
            agreement.title
        )
        assertEquals(
            "Устанавливая PIN-код, вы подтверждаете, что понимаете его назначение как механизма защиты вашей конфиденциальности.",
            agreement.intro
        )
        assertEquals(2, agreement.sections.size)
        assertEquals("1.", agreement.sections[0].number)
        assertEquals("Защитная функция PIN-кода", agreement.sections[0].title)
        assertEquals(
            "Данный PIN-код предназначен для экстренного удаления адресной книги крипто-адресов.",
            agreement.sections[0].description
        )
        assertEquals("2.", agreement.sections[1].number)
        assertEquals("Механизм действия", agreement.sections[1].title)
    }

    @Test
    fun parseDeleteContactsAgreement_missingSections_returnsNull() {
        val agreement = parseDeleteContactsAgreement(
            "Пользовательское соглашение<br/><br/>Текст без структурированных пунктов."
        )

        assertNull(agreement)
    }
}
