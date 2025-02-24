import com.solana.api.Api
import com.solana.core.PublicKey
import com.solana.models.RpcSendTransactionConfig
import com.solana.networking.RpcRequest
import com.solana.networking.serialization.serializers.solana.SolanaResponseSerializer
import com.solana.programs.TokenProgram
import io.horizontalsystems.solanakit.network.makeRequestResultWithRepeat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put

class GetTokenAccountsByOwnerRequest(tokenAccount: PublicKey) : RpcRequest() {
    override val method: String = "getTokenAccountsByOwner"
    override val params = buildJsonArray {
        add(tokenAccount.toString())
        addJsonObject {
            put("programId", TokenProgram.PROGRAM_ID.toString())
        }
        addJsonObject {
            put("encoding", RpcSendTransactionConfig.Encoding.base64.getEncoding())
        }
    }
}

@Serializable
data class SplTokenAccountWithPublicKey(
    // No need other fields
    @SerialName("pubkey") val publicKey: String
)

internal fun GetTokenAccountByOwnerSerializer() =
    SolanaResponseSerializer(ListSerializer(SplTokenAccountWithPublicKey.serializer().nullable))

suspend fun Api.getTokenAccountsByOwner(tokenAccount: PublicKey): Result<List<SplTokenAccountWithPublicKey>> =
    router.makeRequestResultWithRepeat(
        GetTokenAccountsByOwnerRequest(tokenAccount),
        GetTokenAccountByOwnerSerializer()
    ).let { result ->
        @Suppress("UNCHECKED_CAST")
        if (result.isSuccess && result.getOrNull() == null)
            Result.failure(Error("Can not be null"))
        else result as Result<List<SplTokenAccountWithPublicKey>> // safe cast, null case handled above
    }