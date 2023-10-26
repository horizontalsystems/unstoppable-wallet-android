package io.horizontalsystems.bankwallet.modules.walletconnect.version2

import android.util.Log
import com.walletconnect.sign.client.Sign
import com.walletconnect.sign.client.SignClient

class SessionRequestFilterManager() {
    private val TAG = "SessionRequestFilterManager"
    private val rejectList = setOf(
        // custom methods
        "personal_ecRecover",
        "eth_getCode",
        "wallet_switchEthereumChain",
        "wallet_addEthereumChain"
    )

    private fun reject(request: Sign.Model.SessionRequest) {
        try {
            val response = Sign.Params.Response(
                sessionTopic = request.topic,
                jsonRpcResponse = Sign.Model.JsonRpcResponse.JsonRpcError(
                    id = request.request.id,
                    code = 500,
                    message = "Rejected by user"
                )
            )

            SignClient.respond(response) {
                Log.e(TAG, "rejectRequest onError: ", it.throwable)
            }
        } catch (error: Throwable) {
            println(error)
        }
    }

    fun canHandle(request: Sign.Model.SessionRequest): Boolean {
        if (rejectList.contains(request.request.method)) {
            reject(request)
            return true
        }
        return false
    }
}
