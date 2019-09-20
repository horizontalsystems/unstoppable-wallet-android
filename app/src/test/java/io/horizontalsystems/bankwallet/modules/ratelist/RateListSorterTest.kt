package io.horizontalsystems.bankwallet.modules.ratelist

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.horizontalsystems.bankwallet.entities.Coin
import junit.framework.Assert.assertEquals
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object RateListSorterTest: Spek( {

    val rateListSorter by memoized { RateListSorter() }

    val titleBtc = "Bitcoin"
    val codeBtc = "BTC"
    val titleEth = "Ethereum"
    val codeEth = "ETH"
    val titleMco = "Crypto"
    val codeMco = "MCO"
    val titleMana = "Decentraland"
    val codeMana = "MANA"

    val coinBtc = mock<Coin> {
        on { title } doReturn titleBtc
        on { code } doReturn codeBtc
    }
    val coinEth = mock<Coin> {
        on { title } doReturn titleEth
        on { code } doReturn codeEth
    }
    val coinMco = mock<Coin> {
        on { title } doReturn titleMco
        on { code } doReturn codeMco
    }
    val coinMana = mock<Coin> {
        on { title } doReturn titleMana
        on { code } doReturn codeMana
    }


    val featuredCoins = listOf(coinBtc, coinEth)
    val allCoins = listOf(coinBtc, coinEth, coinMco, coinMana)
    val emptyCoins = listOf<Coin>()


    describe("#smartSort") {

        describe("common") {
            it("should return allCoins sorted firstly by featured coins, then remaining by alphabet order") {
                val expected = listOf(coinBtc, coinEth, coinMana, coinMco)
                val actual = rateListSorter.smartSort(allCoins, featuredCoins)
                assertEquals(expected, actual)
            }
        }

        describe("userCoinsIsFeaturedWithDifferentOrder") {
            it("use non-sorted featured if coins subset of featured but in other order") {
                val userCoins = listOf(coinEth, coinBtc)
                val expected = listOf(coinBtc, coinEth)
                val actual = rateListSorter.smartSort(userCoins, featuredCoins)
                assertEquals(expected, actual)
            }
        }

        describe("noEnabledCoins") {
            it("should return only featured coins") {
                rateListSorter.smartSort(emptyCoins, featuredCoins)
                val sorted = rateListSorter.smartSort(emptyCoins, featuredCoins)
                assertEquals(sorted, featuredCoins)
            }
        }
    }
})
