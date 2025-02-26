package io.horizontalsystems.solanakit.noderpc.endpoints

import com.solana.api.Api
import com.solana.api.SignatureInformation
import com.solana.core.PublicKey
import com.solana.networking.RpcRequest
import io.horizontalsystems.solanakit.network.makeRequestResultWithRepeat
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put

class GetConfirmedSignaturesForAddressRequest(
    account: PublicKey,
    limit: Int? = null,
    before: String? = null,
    until: String? = null,
) : RpcRequest() {
    override val method: String = "getSignaturesForAddress"
    override val params = buildJsonArray {
        add(account.toString())
        addJsonObject {
            put("limit", limit?.toLong())
            put("before", before)
            put("until", until)
        }
    }
}

suspend fun Api.getSignaturesForAddress(
    account: PublicKey,
    limit: Int? = null,
    before: String? = null,
    until: String? = null
): Result<List<SignatureInformation>?> {
    return router.makeRequestResultWithRepeat(
        request = GetConfirmedSignaturesForAddressRequest(account, limit, before, until),
        serializer = ListSerializer(SignatureInformation.serializer())
    )
}
