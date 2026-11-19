package io.horizontalsystems.architecture

import org.junit.Assert.assertEquals
import org.junit.Test

class KotlinCommentsTest {

    private val q = "\"\"\""
    private val d = "$"

    @Test
    fun `drops a line comment and keeps the line break`() {
        assertEquals("val a = 1  \nval b = 2", "val a = 1 // one\nval b = 2".withoutComments())
    }

    @Test
    fun `drops a block comment and a KDoc`() {
        assertEquals("  val a =   1", "/** doc */ val a = /* one */ 1".withoutComments())
    }

    @Test
    fun `drops a nested block comment whole`() {
        assertEquals("a   b", "a /* x /* y */ z */ b".withoutComments())
    }

    @Test
    fun `keeps comment markers inside a string`() {
        val code = """val url = "https://example.com/*path*/" + x"""
        assertEquals(code, code.withoutComments())
    }

    @Test
    fun `keeps an escaped quote from ending the string`() {
        val code = """val s = "a \" // b" + x"""
        assertEquals(code, code.withoutComments())
    }

    @Test
    fun `keeps comment markers inside a raw string`() {
        val code = "val s = $q// not a comment \" still inside$q + x"
        assertEquals(code, code.withoutComments())
    }

    @Test
    fun `ends a raw string at the last three quotes of a run`() {
        assertEquals("val s = $q\"a\"$q  ", "val s = $q\"a\"$q // c".withoutComments())
    }

    @Test
    fun `keeps the code after a string nested in a template`() {
        val code = """val pair = "$d{url ?: "https://example.com"}" to Modifier.testTag("send_next")"""
        assertEquals(code, code.withoutComments())
    }

    @Test
    fun `follows braces inside a template`() {
        val code = """val s = "$d{list.map { "//" + it }}" + x"""
        assertEquals(code, code.withoutComments())
    }

    @Test
    fun `drops a comment inside a template`() {
        assertEquals(
            """val s = "$d{a   + b}" + x""",
            """val s = "$d{a /* why */ + b}" + x""".withoutComments()
        )
    }

    @Test
    fun `follows a template inside a raw string`() {
        val code = "val s = $q$d{a ?: \"//\"}$q + x"
        assertEquals(code, code.withoutComments())
    }

    @Test
    fun `keeps a quote character literal from opening a string`() {
        assertEquals("val c = '\"'  ", "val c = '\"' // quote".withoutComments())
    }

    @Test
    fun `keeps an escaped character literal`() {
        assertEquals("val c = '\\''  ", "val c = '\\'' // apostrophe".withoutComments())
    }

    @Test
    fun `keeps a backticked name with quotes and slashes`() {
        val code = "fun `a \"b\" // c`() = 1"
        assertEquals(code, code.withoutComments())
    }

    @Test
    fun `empties a string and keeps its quotes`() {
        assertEquals(
            """val message = "" + x""",
            """val message = "Global\"Scope // a" + x""".withoutCommentsAndLiteralText()
        )
    }

    @Test
    fun `empties a raw string that quotes code`() {
        assertEquals(
            "val doc = $q$q + x",
            "val doc = ${q}Modifier.testTag(\"send_next\")\"$q + x".withoutCommentsAndLiteralText()
        )
    }

    @Test
    fun `keeps the code of a template in an emptied string`() {
        assertEquals(
            """val s = "$d{GlobalScope.hashCode()}" + x""",
            """val s = "a $d{GlobalScope.hashCode()} b" + x""".withoutCommentsAndLiteralText()
        )
    }

    @Test
    fun `keeps the quotes that mark an inline argument`() {
        assertEquals(
            """Modifier.testTag("")""",
            """Modifier.testTag("send_${d}index")""".withoutCommentsAndLiteralText()
        )
    }

    @Test
    fun `survives an unterminated string and comment`() {
        assertEquals("val s = \"abc", "val s = \"abc".withoutComments())
        assertEquals("a  ", "a /* never closed".withoutComments())
    }
}
