package io.horizontalsystems.bankwallet.core

import io.horizontalsystems.bankwallet.core.utils.EthInputParser
import org.junit.Assert.assertEquals
import org.junit.Test

class EthInputParserTest {

    private lateinit var input: String

    @Test
    fun parse_transfer() {
        input = "0xa9059cbb0000000000000000000000005d5724e56ea3cc75352339635960d07c1503f75e00000000000000000000000000000000000000000000003635c9adc5dea00000"
        val data = EthInputParser.parse(input)

        assertEquals(null, data?.from)
        assertEquals("5d5724e56ea3cc75352339635960d07c1503f75e", data?.to)
        assertEquals("00000000000000000000000000000000000000000000003635c9adc5dea00000", data?.value)
    }


    @Test
    fun parse_transferFrom() {
        input = "0x23b872dd00000000000000000000000077fae6ac54240057b443a4d007627710f4e9c12e000000000000000000000000a49e44aa6f7c1752e95e6de0f2043df04bb3f63200000000000000000000000000000000000000000000000000000001cfb778b8"
        val data = EthInputParser.parse(input)

        assertEquals("77fae6ac54240057b443a4d007627710f4e9c12e", data?.from)
        assertEquals("a49e44aa6f7c1752e95e6de0f2043df04bb3f632", data?.to)
        assertEquals("00000000000000000000000000000000000000000000000000000001cfb778b8", data?.value)
    }
}
