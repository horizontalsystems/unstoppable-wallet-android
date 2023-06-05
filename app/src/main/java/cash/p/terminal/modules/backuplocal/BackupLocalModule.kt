package cash.p.terminal.modules.backuplocal

import com.google.gson.annotations.SerializedName
import cash.p.terminal.entities.AccountType

object BackupLocalModule {
    private const val MNEMONIC = "mnemonic"
    private const val PRIVATE_KEY = "private_key"
    private const val ADDRESS = "address"
    private const val SOLANA_ADDRESS = "solana_address"
    private const val HD_EXTENDED_LEY = "hd_extended_key"

    //Backup Json file data structure

    data class WalletBackup(
        val crypto: BackupCrypto,
        val id: String,
        val type: String,
        @SerializedName("manual_backup")
        val manualBackup: Boolean,
        val timestamp: Long,
        val version: Int
    )

    data class BackupCrypto(
        val cipher: String,
        val cipherparams: CipherParams,
        val ciphertext: String,
        val kdf: String,
        val kdfparams: KdfParams,
        val mac: String
    )

    data class CipherParams(
        val iv: String
    )

    class KdfParams(
        val dklen: Int,
        val n: Int,
        val p: Int,
        val r: Int,
        val salt: String
    )

    fun getAccountTypeString(accountType: AccountType): String = when (accountType) {
        is AccountType.Mnemonic -> MNEMONIC
        is AccountType.EvmPrivateKey -> PRIVATE_KEY
        is AccountType.EvmAddress -> ADDRESS
        is AccountType.SolanaAddress -> SOLANA_ADDRESS
        is AccountType.HdExtendedKey -> HD_EXTENDED_LEY
    }

    @Throws(IllegalStateException::class)
    fun getAccountTypeFromString(accountType: String, data: String): AccountType {
        return when (accountType) {
            MNEMONIC -> {
                val parts = data.split("@")
                //check for nonstandard mnemonic from iOs app
                if (parts[0].split("&").size > 1)
                    throw IllegalStateException("Non standard mnemonic")
                val words = parts[0].split(" ")
                val passphrase = if (parts.size > 1) parts[1] else ""
                AccountType.Mnemonic(words, passphrase)
            }

            PRIVATE_KEY -> AccountType.EvmPrivateKey(data.toBigInteger())
            ADDRESS -> AccountType.EvmAddress(data)
            SOLANA_ADDRESS -> AccountType.SolanaAddress(data)
            HD_EXTENDED_LEY -> AccountType.HdExtendedKey(data)
            else -> throw IllegalStateException("Unknown account type")
        }
    }

    fun getStringForEncryption(accountType: AccountType): String = when (accountType) {
        is AccountType.Mnemonic -> {
            val passphrasePart = if (accountType.passphrase.isNotBlank()) {
                "@" + accountType.passphrase
            } else {
                ""
            }
            accountType.words.joinToString(" ") + passphrasePart
        }

        is AccountType.EvmPrivateKey -> accountType.key.toString()
        is AccountType.EvmAddress -> accountType.address
        is AccountType.SolanaAddress -> accountType.address
        is AccountType.HdExtendedKey -> accountType.keySerialized
    }

    val kdfDefault = KdfParams(
        dklen = 32,
        n = 16384,
        p = 4,
        r = 8,
        salt = "unstoppable"
    )
}