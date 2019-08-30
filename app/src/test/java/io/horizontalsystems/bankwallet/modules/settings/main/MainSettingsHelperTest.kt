package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.bankwallet.entities.Currency
import org.junit.Assert
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class MainSettingsHelperTest : Spek({

    val helper = MainSettingsHelper()

    describe("is backed up") {
        context("when non backed up count is 0") {
            it("returns true") {
                Assert.assertTrue(helper.isBackedUp(nonBackedUpCount = 0))
            }
        }

        context("when non backed up count is not 0 ") {
            it("returns false") {
                Assert.assertFalse(helper.isBackedUp(nonBackedUpCount = 1))
            }
        }
    }

    describe("display name for base currency") {
        val currCode = "testCode"
        val currSymbol = "testSymbol"
        val currency = Currency(currCode, currSymbol)

        it("returns currency code") {
            Assert.assertEquals(currCode, helper.displayName(currency))
        }
    }

})
