package io.horizontalsystems.bankwallet.modules.backuplocal

import com.google.gson.annotations.SerializedName
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.CexType
import io.horizontalsystems.hdwalletkit.Base58
import io.horizontalsystems.tronkit.toBigInteger

object BackupLocalModule {
    private const val MNEMONIC = "mnemonic"
    private const val PRIVATE_KEY = "private_key"
    private const val ADDRESS = "address"
    private const val SOLANA_ADDRESS = "solana_address"
    private const val TRON_ADDRESS = "tron_address"
    private const val HD_EXTENDED_LEY = "hd_extended_key"
    private const val CEX = "cex"

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
        is AccountType.TronAddress -> TRON_ADDRESS
        is AccountType.HdExtendedKey -> HD_EXTENDED_LEY
        is AccountType.Cex -> CEX
    }

    @Throws(IllegalStateException::class)
    fun getAccountTypeFromData(accountType: String, data: ByteArray): AccountType {
        return when (accountType) {
            MNEMONIC -> {
                val parts = String(data, Charsets.UTF_8).split("@", limit = 2)
                //check for nonstandard mnemonic from iOs app
                if (parts[0].split("&").size > 1)
                    throw IllegalStateException("Non standard mnemonic")
                val words = parts[0].split(" ")
                val passphrase = if (parts.size > 1) parts[1] else ""
                AccountType.Mnemonic(words, passphrase)
            }

            PRIVATE_KEY -> AccountType.EvmPrivateKey(data.toBigInteger())
            ADDRESS -> AccountType.EvmAddress(String(data, Charsets.UTF_8))
            SOLANA_ADDRESS -> AccountType.SolanaAddress(String(data, Charsets.UTF_8))
            TRON_ADDRESS -> AccountType.TronAddress(String(data, Charsets.UTF_8))
            HD_EXTENDED_LEY -> AccountType.HdExtendedKey(Base58.encode(data))
            CEX -> {
                val cexType = CexType.deserialize(String(data, Charsets.UTF_8))
                if (cexType != null) {
                    AccountType.Cex(cexType)
                } else {
                    throw IllegalStateException("Unknown Cex account type")
                }
            }

            else -> throw IllegalStateException("Unknown account type")
        }
    }

    fun getDataForEncryption(accountType: AccountType): ByteArray = when (accountType) {
        is AccountType.Mnemonic -> {
            val passphrasePart = if (accountType.passphrase.isNotBlank()) {
                "@" + accountType.passphrase
            } else {
                ""
            }
            val combined = accountType.words.joinToString(" ") + passphrasePart
            combined.toByteArray(Charsets.UTF_8)
        }

        is AccountType.EvmPrivateKey -> accountType.key.toByteArray()
        is AccountType.EvmAddress -> accountType.address.toByteArray(Charsets.UTF_8)
        is AccountType.SolanaAddress -> accountType.address.toByteArray(Charsets.UTF_8)
        is AccountType.TronAddress -> accountType.address.toByteArray(Charsets.UTF_8)
        is AccountType.HdExtendedKey -> Base58.decode(accountType.keySerialized)
        is AccountType.Cex -> accountType.cexType.serialized().toByteArray(Charsets.UTF_8)
    }

    val kdfDefault = KdfParams(
        dklen = 32,
        n = 16384,
        p = 4,
        r = 8,
        salt = App.appConfigProvider.accountsBackupFileSalt
    )
}