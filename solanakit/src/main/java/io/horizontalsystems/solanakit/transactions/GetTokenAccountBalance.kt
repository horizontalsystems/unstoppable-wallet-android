import com.solana.api.Api
import com.solana.api.GetTokenAccountBalanceRequest
import com.solana.api.TokenAmountInfoResponse
import com.solana.core.PublicKey
import com.solana.networking.serialization.serializers.solana.SolanaResponseSerializer
import io.horizontalsystems.solanakit.network.makeRequestResultWithRepeat

internal fun GetTokenAccountBalanceSerializer() =
    SolanaResponseSerializer(TokenAmountInfoResponse.serializer())

suspend fun Api.getTokenAccountBalanceWithRepeat(tokenAccount: PublicKey): Result<TokenAmountInfoResponse> =
    router.makeRequestResultWithRepeat(
        GetTokenAccountBalanceRequest(tokenAccount),
        GetTokenAccountBalanceSerializer()
    ).let { result ->
        @Suppress("UNCHECKED_CAST")
        if (result.isSuccess && result.getOrNull() == null)
            Result.failure(Error("Can not be null"))
        else result as Result<TokenAmountInfoResponse> // safe cast, null case handled above
    }