package cash.p.terminal.modules.backuplocal

import com.google.gson.annotations.SerializedName
import cash.p.terminal.core.App
import cash.p.terminal.core.managers.RestoreSettingType
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.CexType
import io.horizontalsystems.hdwalletkit.Base58
import io.horizontalsystems.tronkit.toBigInteger

object BackupLocalModule {
    private const val MNEMONIC = "mnemonic"
    private const val PRIVATE_KEY = "private_key"
    private const val ADDRESS = "evm_address"
    private const val SOLANA_ADDRESS = "solana_address"
    private const val TRON_ADDRESS = "tron_address"
    private const val TON_ADDRESS = "ton_address"
    private const val BITCOIN_ADDRESS = "bitcoin_address"
    private const val HD_EXTENDED_LEY = "hd_extended_key"
    private const val CEX = "cex"

    //Backup Json file data structure

    data class WalletBackup(
        val crypto: BackupCrypto,
        val id: String,
        val type: String,
        @SerializedName("enabled_wallets")
        val enabledWallets: List<EnabledWalletBackup>?,
        @SerializedName("manual_backup")
        val manualBackup: Boolean,
        @SerializedName("file_backup")
        val fileBackup: Boolean,
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

    data class EnabledWalletBackup(
        @SerializedName("token_query_id")
        val tokenQueryId: String,
        @SerializedName("coin_name")
        val coinName: String? = null,
        @SerializedName("coin_code")
        val coinCode: String? = null,
        val decimals: Int? = null,
        val settings: Map<RestoreSettingType, String>?
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

    fun getAccountTypeString(accountType: cash.p.terminal.wallet.AccountType): String = when (accountType) {
        is cash.p.terminal.wallet.AccountType.Mnemonic -> MNEMONIC
        is cash.p.terminal.wallet.AccountType.EvmPrivateKey -> PRIVATE_KEY
        is cash.p.terminal.wallet.AccountType.EvmAddress -> ADDRESS
        is cash.p.terminal.wallet.AccountType.SolanaAddress -> SOLANA_ADDRESS
        is cash.p.terminal.wallet.AccountType.TronAddress -> TRON_ADDRESS
        is cash.p.terminal.wallet.AccountType.TonAddress -> TON_ADDRESS
        is cash.p.terminal.wallet.AccountType.BitcoinAddress -> BITCOIN_ADDRESS
        is cash.p.terminal.wallet.AccountType.HdExtendedKey -> HD_EXTENDED_LEY
        is cash.p.terminal.wallet.AccountType.Cex -> CEX
    }

    @Throws(IllegalStateException::class)
    fun getAccountTypeFromData(accountType: String, data: ByteArray): cash.p.terminal.wallet.AccountType {
        return when (accountType) {
            MNEMONIC -> {
                val parts = String(data, Charsets.UTF_8).split("@", limit = 2)
                //check for nonstandard mnemonic from iOs app
                if (parts[0].split("&").size > 1)
                    throw IllegalStateException("Non standard mnemonic")
                val words = parts[0].split(" ")
                val passphrase = if (parts.size > 1) parts[1] else ""
                cash.p.terminal.wallet.AccountType.Mnemonic(words, passphrase)
            }

            PRIVATE_KEY -> cash.p.terminal.wallet.AccountType.EvmPrivateKey(data.toBigInteger())
            ADDRESS -> cash.p.terminal.wallet.AccountType.EvmAddress(String(data, Charsets.UTF_8))
            SOLANA_ADDRESS -> cash.p.terminal.wallet.AccountType.SolanaAddress(String(data, Charsets.UTF_8))
            TRON_ADDRESS -> cash.p.terminal.wallet.AccountType.TronAddress(String(data, Charsets.UTF_8))
            TON_ADDRESS -> cash.p.terminal.wallet.AccountType.TonAddress(String(data, Charsets.UTF_8))
            BITCOIN_ADDRESS -> cash.p.terminal.wallet.AccountType.BitcoinAddress.fromSerialized(String(data, Charsets.UTF_8))
            HD_EXTENDED_LEY -> cash.p.terminal.wallet.AccountType.HdExtendedKey(Base58.encode(data))
            CEX -> {
                val cexType = cash.p.terminal.wallet.CexType.deserialize(String(data, Charsets.UTF_8))
                if (cexType != null) {
                    cash.p.terminal.wallet.AccountType.Cex(cexType)
                } else {
                    throw IllegalStateException("Unknown Cex account type")
                }
            }

            else -> throw IllegalStateException("Unknown account type")
        }
    }

    fun getDataForEncryption(accountType: cash.p.terminal.wallet.AccountType): ByteArray = when (accountType) {
        is cash.p.terminal.wallet.AccountType.Mnemonic -> {
            val passphrasePart = if (accountType.passphrase.isNotBlank()) {
                "@" + accountType.passphrase
            } else {
                ""
            }
            val combined = accountType.words.joinToString(" ") + passphrasePart
            combined.toByteArray(Charsets.UTF_8)
        }

        is cash.p.terminal.wallet.AccountType.EvmPrivateKey -> accountType.key.toByteArray()
        is cash.p.terminal.wallet.AccountType.EvmAddress -> accountType.address.toByteArray(Charsets.UTF_8)
        is cash.p.terminal.wallet.AccountType.SolanaAddress -> accountType.address.toByteArray(Charsets.UTF_8)
        is cash.p.terminal.wallet.AccountType.TronAddress -> accountType.address.toByteArray(Charsets.UTF_8)
        is cash.p.terminal.wallet.AccountType.TonAddress -> accountType.address.toByteArray(Charsets.UTF_8)
        is cash.p.terminal.wallet.AccountType.BitcoinAddress -> accountType.serialized.toByteArray(Charsets.UTF_8)
        is cash.p.terminal.wallet.AccountType.HdExtendedKey -> Base58.decode(accountType.keySerialized)
        is cash.p.terminal.wallet.AccountType.Cex -> accountType.cexType.serialized().toByteArray(Charsets.UTF_8)
    }

    val kdfDefault = KdfParams(
        dklen = 32,
        n = 16384,
        p = 4,
        r = 8,
        salt = App.appConfigProvider.accountsBackupFileSalt
    )
}