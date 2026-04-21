package cash.p.terminal.wallet

import android.os.Parcelable
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.HardwarePublicKey
import cash.p.terminal.wallet.entities.HardwarePublicKeyType
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.policy.HardwareWalletTokenPolicy
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.hdwalletkit.HDExtendedKey
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.util.Objects

@ConsistentCopyVisibility
@Parcelize
data class Wallet internal constructor(
    val token: Token,
    val account: Account,
    val hardwarePublicKey: HardwarePublicKey? // used only for hardware wallets
) : Parcelable {
    val coin
        get() = token.coin

    val decimal
        get() = token.decimals

    val badge
        get() = token.badge

    val transactionSource get() = TransactionSource(token.blockchain, account, token.type.meta)

    override fun equals(other: Any?): Boolean {
        if (other is Wallet) {
            return token == other.token && account == other.account && hardwarePublicKey == other.hardwarePublicKey
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(token, account, hardwarePublicKey)
    }

    fun getHDExtendedKey(): HDExtendedKey? {
        return hardwarePublicKey?.key?.value?.let {
            if (hardwarePublicKey.type == HardwarePublicKeyType.PUBLIC_KEY) {
                HDExtendedKey(it)
            } else {
                null
            }
        }
    }
}

val Wallet.tokenQueryId: String
    get() = token.tokenQuery.id

class WalletFactory(
    private val hardwareWalletTokenPolicy: HardwareWalletTokenPolicy
) {

    companion object {
        /***
         * Preview wallet for Compose previews
         */
        fun previewWallet(): Wallet {
            val token = Token(
                coin = Coin("Preview Coin", "PCN", "code"),
                blockchain = Blockchain(
                    type = BlockchainType.Ethereum,
                    name = "Ethereum",
                    eip3091url = null
                ),
                type = TokenType.Native,
                decimals = 8
            )
            val account = Account(
                id = "preview-account-id",
                name = "Preview Account",
                type = AccountType.EvmAddress("0x"),
                origin = AccountOrigin.Created,
                level = 0
            )
            return Wallet(token, account, null)
        }
    }

    fun create(token: Token, account: Account, hardwarePublicKey: HardwarePublicKey?): Wallet? {
        if (!account.type.isCompatibleWith(token.blockchainType, token.type)) {
            Timber.d(
                "Skipping wallet creation for token ${token.blockchainType} ${token.type} - account type ${account.type::class.simpleName} is not supported"
            )
            return null
        }

        if (account.type is AccountType.HardwareCard &&
            !hardwareWalletTokenPolicy.isSupported(token)
        ) {
            Timber.d(
                "Skipping wallet creation for token ${token.blockchainType} ${token.type} - hardware wallet not supported"
            )
            return null
        }

        return Wallet(token, account, hardwarePublicKey)
    }
}
