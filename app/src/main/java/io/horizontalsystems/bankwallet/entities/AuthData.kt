package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.hdwalletkit.Mnemonic
import java.util.*

data class AuthData(val words: List<String> = Mnemonic().generate(), val walletId: String = UUID.randomUUID().toString())
