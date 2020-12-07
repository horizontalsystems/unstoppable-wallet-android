package io.horizontalsystems.bankwallet.core.adapters

import org.junit.Test

class EosAdapterTest {

    @Test
    fun testValidate() {
        EosAdapter.validateAccountName("wwwsalmonco")
        EosAdapter.validateAccountName("wwwsalmoncom")
        EosAdapter.validateAccountName("wwwsalmoncoma")
        EosAdapter.validateAccountName("abc")
        EosAdapter.validateAccountName("a.b")
        EosAdapter.validateAccountName(".a")
        EosAdapter.validateAccountName("booleanjulie")
        EosAdapter.validateAccountName("tbcox123")
        EosAdapter.validateAccountName("1.3.5")
    }

    @Test(expected = EosError.InvalidAccountName::class)
    fun testValidate_Invalid1() {
        EosAdapter.validateAccountName("wwwsalmonc.")
    }

    @Test(expected = EosError.InvalidAccountName::class)
    fun testValidate_Invalid2() {
        EosAdapter.validateAccountName("Wwwsalmoncom")
    }

    @Test(expected = EosError.InvalidAccountName::class)
    fun testValidate_Invalid3() {
        EosAdapter.validateAccountName(".")
    }

    @Test(expected = EosError.InvalidAccountName::class)
    fun testValidate_Invalid4() {
        EosAdapter.validateAccountName("aa.")
    }

    @Test(expected = EosError.InvalidAccountName::class)
    fun testValidate_Invalid5() {
        EosAdapter.validateAccountName("booleanjulien")
    }

    @Test(expected = EosError.InvalidAccountName::class)
    fun testValidate_Invalid6() {
        EosAdapter.validateAccountName("tbvox456")
    }

    @Test(expected = EosError.InvalidAccountName::class)
    fun testValidate_Invalid7() {
        EosAdapter.validateAccountName("tbVOX")
    }

}
