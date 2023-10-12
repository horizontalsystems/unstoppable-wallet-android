package io.horizontalsystems.bankwallet.modules.restoreaccount.restoreprivatekey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.hexToByteArray
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreprivatekey.RestorePrivateKeyModule.RestoreError.EmptyText
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreprivatekey.RestorePrivateKeyModule.RestoreError.NoValidKey
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreprivatekey.RestorePrivateKeyModule.RestoreError.NonPrivateKey
import io.horizontalsystems.bankwallet.modules.restoreaccount.restoreprivatekey.RestorePrivateKeyModule.RestoreError.NotSupportedDerivedType
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import java.math.BigInteger

class RestorePrivateKeyViewModel(
    accountFactory: IAccountFactory,
) : ViewModel() {

    val defaultName = accountFactory.getNextAccountName()
    var accountName: String = defaultName
        get() = field.ifBlank { defaultName }
        private set

    private var text = ""

    var inputState by mutableStateOf<DataState.Error?>(null)
        private set

    fun onEnterName(name: String) {
        accountName = name
    }

    fun onEnterPrivateKey(input: String) {
        inputState = null
        text = input
    }

    fun resolveAccountType(): AccountType? {
        inputState = null
        return try {
            accountType(text)
        } catch (e: Exception) {
            inputState = DataState.Error(
                Exception(Translator.getString(R.string.Restore_PrivateKey_InvalidKey))
            )
            null
        }
    }

    @Throws(Exception::class)
    private fun accountType(text: String): AccountType {
        val textCleaned = text.trim()

        if (textCleaned.isEmpty()) {
            throw EmptyText
        }

        if (isValidEthereumPrivateKey(textCleaned)) {
            val privateKey = Signer.privateKey(textCleaned)
            return AccountType.EvmPrivateKey(privateKey)
        }

        try {
            val extendedKey = HDExtendedKey(textCleaned)
            if (extendedKey.isPublic) {
                throw NonPrivateKey
            }
            when (extendedKey.derivedType) {
                HDExtendedKey.DerivedType.Master,
                HDExtendedKey.DerivedType.Account -> {
                    return AccountType.HdExtendedKey(extendedKey.serializePrivate())
                }

                else -> throw NotSupportedDerivedType
            }
        } catch (e: Throwable) {
            throw NoValidKey
        }
    }

    private fun isValidEthereumPrivateKey(privateKeyHex: String): Boolean {
        try {
            //key should be 32 bytes long
            privateKeyHex.hexToByteArray().let {
                if (it.size != 32) {
                    return false
                }
            }

            // Convert the hex private key to a BigInteger
            val privateKeyBigInt = BigInteger(privateKeyHex, 16)

            // Define the order of the secp256k1 curve (n)
            val secp256k1Order = BigInteger(
                "fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141",
                16
            )

            // Check if the private key is greater than zero and less than the order
            return privateKeyBigInt > BigInteger.ZERO && privateKeyBigInt < secp256k1Order
        } catch (e: NumberFormatException) {
            return false
        }
    }
}
