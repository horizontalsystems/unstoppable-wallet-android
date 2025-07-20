package cash.p.terminal.wallet.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity(
    foreignKeys = [ForeignKey(
        entity = AccountRecord::class,
        parentColumns = ["id"],
        childColumns = ["accountId"],
        onUpdate = ForeignKey.CASCADE,
        onDelete = ForeignKey.CASCADE,
        deferred = true
    )],
    indices = [Index("accountId", name = "index_HardwarePublicKey_accountId")]
)
@Parcelize
class HardwarePublicKey(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: String,
    val blockchainType: String,
    val type: HardwarePublicKeyType,
    val tokenType: TokenType,
    val key: SecretString,
    val derivationPath: String,
    val publicKey: ByteArray,
    val derivedPublicKey: ByteArray
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HardwarePublicKey

        if (id != other.id) return false
        if (accountId != other.accountId) return false
        if (blockchainType != other.blockchainType) return false
        if (type != other.type) return false
        if (tokenType != other.tokenType) return false
        if (key != other.key) return false
        if (derivationPath != other.derivationPath) return false
        if (!publicKey.contentEquals(other.publicKey)) return false
        if (!derivedPublicKey.contentEquals(other.derivedPublicKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + accountId.hashCode()
        result = 31 * result + blockchainType.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + tokenType.hashCode()
        result = 31 * result + key.hashCode()
        result = 31 * result + derivationPath.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + derivedPublicKey.contentHashCode()
        return result
    }
}

enum class HardwarePublicKeyType {
    PUBLIC_KEY,
    ADDRESS
}
