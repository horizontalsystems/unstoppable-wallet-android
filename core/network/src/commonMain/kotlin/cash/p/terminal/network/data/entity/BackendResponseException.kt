package cash.p.terminal.network.data.entity

class BackendResponseException(
    val errorData: BackendResponseError,
) : Exception(errorData.message ?: "Unknown backend error")