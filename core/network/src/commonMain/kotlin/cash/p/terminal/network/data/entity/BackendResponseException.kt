package cash.p.terminal.network.data.entity

class BackendResponseException(
    errorData: BackendResponseError,
) : Exception(errorData.message ?: "Unknown backend error")