package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.Error
import io.horizontalsystems.bankwallet.entities.CoinType
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CoinTypeConverterTest {

    lateinit var converter: CoinTypeConverter

    @Before
    fun setUp() {
        converter = CoinTypeConverter()
    }

    @Test
    fun coinTypeToString_bitcoin() {
        val bitcoinType = CoinType.Bitcoin
        val bitcoinString = converter.coinTypeToString(bitcoinType)
        val expectedString = "bitcoin_key"
        Assert.assertEquals(expectedString, bitcoinString)
    }

    @Test
    fun coinTypeToString_erc20() {
        val erc20Type = CoinType.Erc20("address", 12)
        val erc20String = converter.coinTypeToString(erc20Type)
        print("erc: $erc20String")
        val expectedString = "erc_20_key;address;12"
        Assert.assertEquals(expectedString, erc20String)
    }

    @Test
    fun stringToCoinType_bitcoin() {
        val bitcoinString = "bitcoin_key"
        val bitconType: CoinType = converter.stringToCoinType(bitcoinString)
        val bitcoinType = CoinType.Bitcoin
        Assert.assertEquals(bitcoinType, bitconType)
    }

    @Test
    fun stringToCoinType_erc20() {
        val erc20String = "erc_20_key;address;12"
        val erc20Type: CoinType = converter.stringToCoinType(erc20String)
        Assert.assertTrue(erc20Type is CoinType.Erc20)
    }

    @Test(expected = Error.CoinTypeException::class)
    fun stringToCoinType_erc20_failure() {
        val erc20String = "erc_20_key;address;"
        converter.stringToCoinType(erc20String)
    }
}
