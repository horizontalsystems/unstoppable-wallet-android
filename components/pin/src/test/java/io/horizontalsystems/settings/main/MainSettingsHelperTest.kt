package io.horizontalsystems.bankwallet.modules.settings.main

import io.horizontalsystems.bankwallet.entities.Currency
import org.junit.Assert
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class MainSettingsHelperTest : Spek({

    val helper = MainSettingsHelper()

    describe("display name for base currency") {
        val currCode = "testCode"
        val currSymbol = "testSymbol"
        val currency = Currency(currCode, currSymbol)

        it("returns currency code") {
            Assert.assertEquals(currCode, helper.displayName(currency))
        }
    }

})
