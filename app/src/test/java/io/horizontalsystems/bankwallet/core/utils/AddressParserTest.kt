package io.horizontalsystems.bankwallet.core.utils

import io.horizontalsystems.bankwallet.entities.AddressData
import org.junit.Assert
import org.junit.Test

class AddressParserTest {
    private lateinit var addressParser: AddressParser

    @Test
    fun parse_BitcoinPaymentAddress() {
        addressParser = AddressParser("bitcoin", true)

        var paymentData = AddressData(address = "address_data")
        checkPaymentData(addressParser, "address_data", paymentData)

        // Check bitcoin addresses parsing with drop scheme if it's valid
        paymentData = AddressData(address = "address_data")
        checkPaymentData(addressParser, "bitcoin:address_data", paymentData)

        // invalid scheme - need to keep scheme
        paymentData = AddressData(address = "bitcoincash:address_data")
        checkPaymentData(addressParser, "bitcoincash:address_data", paymentData)

        // check parameters
        paymentData = AddressData(address = "address_data", version = "1.0")
        checkPaymentData(addressParser, "address_data;version=1.0", paymentData)

        paymentData = AddressData(address = "address_data", version = "1.0", label = "test")
        checkPaymentData(addressParser, "bitcoin:address_data;version=1.0?label=test", paymentData)

        paymentData = AddressData(address = "address_data", amount = 0.01)
        checkPaymentData(addressParser, "bitcoin:address_data?amount=0.01", paymentData)

        paymentData = AddressData(address = "address_data", amount = 0.01, label = "test_sender")
        checkPaymentData(addressParser, "bitcoin:address_data?amount=0.01?label=test_sender", paymentData)

        paymentData = AddressData(address = "address_data", parameters = mutableMapOf("custom" to "any"))
        checkPaymentData(addressParser, "bitcoin:address_data?custom=any", paymentData)
    }

    @Test
    fun parse_BitcoinCashPaymentAddress() {
        addressParser = AddressParser("bitcoincash", false)

        var paymentData = AddressData(address = "address_data")
        checkPaymentData(addressParser, "address_data", paymentData)

        // Check bitcoincash addresses parsing with keep scheme if it's valid
        paymentData = AddressData(address = "bitcoincash:address_data")
        checkPaymentData(addressParser, "bitcoincash:address_data", paymentData)

        // invalid scheme - need to leave scheme
        paymentData = AddressData(address = "bitcoin:address_data")
        checkPaymentData(addressParser, "bitcoin:address_data", paymentData)

        // check parameters
        paymentData = AddressData(address = "address_data", version = "1.0")
        checkPaymentData(addressParser, "address_data;version=1.0", paymentData)

        paymentData = AddressData(address = "bitcoincash:address_data", version = "1.0", label = "test")
        checkPaymentData(addressParser, "bitcoincash:address_data;version=1.0?label=test", paymentData)

        paymentData = AddressData(address = "bitcoincash:address_data", amount = 0.01)
        checkPaymentData(addressParser, "bitcoincash:address_data?amount=0.01", paymentData)

        paymentData = AddressData(address = "bitcoincash:address_data", amount = 0.01, label = "test_sender")
        checkPaymentData(addressParser, "bitcoincash:address_data?amount=0.01?label=test_sender", paymentData)

        paymentData = AddressData(address = "bitcoincash:address_data", parameters = mutableMapOf("custom" to "any"))
        checkPaymentData(addressParser, "bitcoincash:address_data?custom=any", paymentData)
    }

    @Test
    fun parse_EthereumPaymentAddress() {
        addressParser = AddressParser("ethereum", true)

        var paymentData = AddressData(address = "address_data")
        checkPaymentData(addressParser, "address_data", paymentData)

        // Check bitcoin addresses parsing with drop scheme if it's valid
        paymentData = AddressData(address = "address_data")
        checkPaymentData(addressParser, "ethereum:address_data", paymentData)

        // invalid scheme - need to keep scheme
        paymentData = AddressData(address = "bitcoincash:address_data")
        checkPaymentData(addressParser, "bitcoincash:address_data", paymentData)

        // check parameters
        paymentData = AddressData(address = "address_data", version = "1.0")
        checkPaymentData(addressParser, "address_data;version=1.0", paymentData)

        paymentData = AddressData(address = "address_data", version = "1.0", label = "test")
        checkPaymentData(addressParser, "ethereum:address_data;version=1.0?label=test", paymentData)

        paymentData = AddressData(address = "address_data", amount = 0.01)
        checkPaymentData(addressParser, "ethereum:address_data?amount=0.01", paymentData)

        paymentData = AddressData(address = "address_data", amount = 0.01, label = "test_sender")
        checkPaymentData(addressParser, "ethereum:address_data?amount=0.01?label=test_sender", paymentData)

        paymentData = AddressData(address = "address_data", parameters = mutableMapOf("custom" to "any"))
        checkPaymentData(addressParser, "ethereum:address_data?custom=any", paymentData)
    }

    private fun checkPaymentData(addressParser: AddressParser, paymentAddress: String, paymentData: AddressData) {
        val bitcoinPaymentData = addressParser.parse(paymentAddress)
        Assert.assertEquals(bitcoinPaymentData, paymentData)
    }
}
