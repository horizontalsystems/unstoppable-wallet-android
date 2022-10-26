package io.horizontalsystems.bankwallet.modules.restoreaccount.resoreprivatekey

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAccountFactory
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.restoreaccount.resoreprivatekey.RestorePrivateKeyModule.RestoreError.*
import io.horizontalsystems.ethereumkit.core.signer.Signer
import io.horizontalsystems.hdwalletkit.HDExtendedKey

class RestorePrivateKeyViewModel(
    accountFactory: IAccountFactory,
) : ViewModel() {

    val defaultName = accountFactory.getNextAccountName()
    private var text = ""

    var inputState by mutableStateOf<DataState.Error?>(null)
        private set

    fun onEnterPrivateKey(input: String) {
        inputState = null
        text = input
    }

    fun resolveAccountType(): AccountType? {
        inputState = null
        return try {
            accountType(text)
        } catch (e: Throwable) {
            inputState = DataState.Error(
                Exception(Translator.getString(R.string.Restore_PrivateKey_InvalidKey))
            )
            null
        }
    }

    private fun accountType(text: String): AccountType {
        val textCleaned = text.trim()

        if (textCleaned.isEmpty()) {
            throw EmptyText
        }

        try {
            val extendedKey = HDExtendedKey(textCleaned)
            if (!extendedKey.info.isPublic) {
                when (extendedKey.derivedType) {
                    HDExtendedKey.DerivedType.Master,
                    HDExtendedKey.DerivedType.Account -> {
                        return AccountType.HdExtendedKey(extendedKey.serializePrivate())
                    }
                    else -> throw NotSupportedDerivedType
                }
            } else {
                throw NonPrivateKey
            }
        } catch (e: Throwable) {
            //do nothing
        }

        try {
            val privateKey = Signer.privateKey(text)
            return AccountType.EvmPrivateKey(privateKey)
        } catch (e: Throwable) {
            //do nothing
        }

        throw NoValidKey
    }
}
