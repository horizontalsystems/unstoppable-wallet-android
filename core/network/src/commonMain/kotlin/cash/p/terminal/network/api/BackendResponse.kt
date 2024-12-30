package cash.p.terminal.network.api

import cash.p.terminal.network.data.entity.BackendResponseError
import cash.p.terminal.network.data.entity.BackendResponseException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

internal suspend inline fun <reified T : Any> HttpResponse.parseResponse(): T {
    return if (status.isSuccess()) {
        body<T>()
    } else {
        throw BackendResponseException(body<BackendResponseError>())
    }
}