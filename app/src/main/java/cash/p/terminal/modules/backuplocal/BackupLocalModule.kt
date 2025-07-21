package cash.p.terminal.modules.backuplocal

import cash.p.terminal.core.App
import cash.p.terminal.core.getKoinInstance
import cash.p.terminal.core.managers.RestoreSettingType
import cash.p.terminal.core.usecase.MoneroWalletUseCase
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.CexType
import com.google.gson.annotations.SerializedName
import io.horizontalsystems.hdwalletkit.Base58
import io.horizontalsystems.tronkit.toBigInteger

object BackupLocalModule {
    private const val MNEMONIC = "mnemonic"
    private const val MNEMONIC_MONERO = "mnemonic_monero"
    private const val PRIVATE_KEY = "private_key"
    private const val SECRET_KEY = "secret_key"
    private const val ADDRESS = "evm_address"
    private const val SOLANA_ADDRESS = "solana_address"
    private const val TRON_ADDRESS = "tron_address"
    private const val TON_ADDRESS = "ton_address"
    private const val STELLAR_ADDRESS = "stellar_address"
    private const val BITCOIN_ADDRESS = "bitcoin_address"
    private const val HD_EXTENDED_KEY = "hd_extended_key"
    private const val UFVK = "ufvk"
    private const val HARDWARE_CARD = "hardware_card"
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

    fun getAccountTypeString(accountType: AccountType): String = when (accountType) {
        is AccountType.Mnemonic -> MNEMONIC
        is AccountType.MnemonicMonero -> MNEMONIC_MONERO
        is AccountType.EvmPrivateKey -> PRIVATE_KEY
        is AccountType.StellarSecretKey -> SECRET_KEY
        is AccountType.EvmAddress -> ADDRESS
        is AccountType.SolanaAddress -> SOLANA_ADDRESS
        is AccountType.TronAddress -> TRON_ADDRESS
        is AccountType.TonAddress -> TON_ADDRESS
        is AccountType.StellarAddress -> STELLAR_ADDRESS
        is AccountType.BitcoinAddress -> BITCOIN_ADDRESS
        is AccountType.HdExtendedKey -> HD_EXTENDED_KEY
        is AccountType.ZCashUfvKey -> UFVK
        is AccountType.Cex -> CEX
        is AccountType.HardwareCard -> HARDWARE_CARD
    }

    @Throws(IllegalStateException::class)
    suspend fun getAccountTypeFromData(accountType: String, data: ByteArray): AccountType {
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

            MNEMONIC_MONERO -> {
                val parts = String(data, Charsets.UTF_8).split("@", limit = 3)
                if(parts.size != 2) {
                    throw IllegalStateException("Wrong monero backup format")
                }
                val words = parts[0].split(" ")
                val height = parts[1].toLong()
                val generateMoneroWalletUseCase: MoneroWalletUseCase = getKoinInstance()
                return generateMoneroWalletUseCase.restore(
                    words,
                    height,
                ) ?: throw IllegalStateException("Wallet is empty")
            }

            PRIVATE_KEY -> AccountType.EvmPrivateKey(data.toBigInteger())
            SECRET_KEY -> AccountType.StellarSecretKey(String(data, Charsets.UTF_8))
            ADDRESS -> AccountType.EvmAddress(String(data, Charsets.UTF_8))
            SOLANA_ADDRESS -> AccountType.SolanaAddress(String(data, Charsets.UTF_8))
            TRON_ADDRESS -> AccountType.TronAddress(String(data, Charsets.UTF_8))
            TON_ADDRESS -> AccountType.TonAddress(String(data, Charsets.UTF_8))
            STELLAR_ADDRESS -> AccountType.StellarAddress(String(data, Charsets.UTF_8))
            BITCOIN_ADDRESS -> AccountType.BitcoinAddress.fromSerialized(
                String(
                    data,
                    Charsets.UTF_8
                )
            )

            HD_EXTENDED_KEY -> AccountType.HdExtendedKey(Base58.encode(data))
            UFVK -> AccountType.ZCashUfvKey(String(data, Charsets.UTF_8))
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

        is AccountType.MnemonicMonero -> {
            val combined = listOf(
                accountType.words.joinToString(" "),
                accountType.height.toString()
            ).joinToString("@")
            combined.toByteArray(Charsets.UTF_8)
        }

        is AccountType.EvmPrivateKey -> accountType.key.toByteArray()
        is AccountType.StellarSecretKey -> accountType.key.toByteArray(Charsets.UTF_8)
        is AccountType.EvmAddress -> accountType.address.toByteArray(Charsets.UTF_8)
        is AccountType.SolanaAddress -> accountType.address.toByteArray(Charsets.UTF_8)
        is AccountType.TronAddress -> accountType.address.toByteArray(Charsets.UTF_8)
        is AccountType.TonAddress -> accountType.address.toByteArray(Charsets.UTF_8)
        is AccountType.StellarAddress -> accountType.address.toByteArray(Charsets.UTF_8)
        is AccountType.BitcoinAddress -> accountType.serialized.toByteArray(Charsets.UTF_8)
        is AccountType.HdExtendedKey -> Base58.decode(accountType.keySerialized)
        is AccountType.Cex -> accountType.cexType.serialized().toByteArray(Charsets.UTF_8)
        is AccountType.ZCashUfvKey -> accountType.key.toByteArray(Charsets.UTF_8)
        is AccountType.HardwareCard -> {
            (accountType.cardId + "@" + accountType.walletPublicKey).toByteArray(Charsets.UTF_8)
        }
    }

    val kdfDefault = KdfParams(
        dklen = 32,
        n = 16384,
        p = 4,
        r = 8,
        salt = App.appConfigProvider.accountsBackupFileSalt
    )
}