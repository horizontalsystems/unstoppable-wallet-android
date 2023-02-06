package io.horizontalsystems.marketkit.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class TokenType : Parcelable {

    @Parcelize
    object Native : TokenType()

    @Parcelize
    data class Eip20(val address: String) : TokenType()

    @Parcelize
    data class Bep2(val symbol: String) : TokenType()

    @Parcelize
    data class Spl(val address: String) : TokenType()

    @Parcelize
    data class Unsupported(val type: String, val reference: String?) : TokenType()

    val id: String
        get() = when (this) {
            Native -> "native"
            is Eip20 -> listOf("eip20", address).joinToString(":")
            is Bep2 -> listOf("bep2", symbol).joinToString(":")
            is Spl -> listOf("spl", address).joinToString(":")
            is Unsupported -> if (reference != null) {
                listOf("unsupported", type, reference).joinToString(":")
            } else {
                listOf("unsupported", type).joinToString(":")
            }
        }

    val values: Value
        get() = when (this) {
            is Native -> Value("native", null)
            is Eip20 -> Value("eip20", address)
            is Bep2 -> Value("bep2", symbol)
            is Spl -> Value("spl", address)
            is Unsupported -> Value(type, reference)
        }

    data class Value(
        val type: String,
        val reference: String?
    )

    companion object {

        fun fromType(type: String, reference: String? = null): TokenType {
            when (type) {
                "native" -> return Native

                "eip20" -> {
                    if (reference != null) {
                        return Eip20(reference)
                    }
                }

                "bep2" -> {
                    if (reference != null) {
                        return Bep2(reference)
                    }
                }

                "spl" -> {
                    if (reference != null) {
                        return Spl(reference)
                    }
                }

                else -> {}
            }

            return Unsupported(type, reference)
        }

        fun fromId(id: String): TokenType? {
            val chunks = id.split(":")

            return when (chunks[0]) {
                "native" -> Native
                "eip20" -> chunks.getOrNull(1)?.let {
                    Eip20(it)
                }
                "bep2" -> chunks.getOrNull(1)?.let {
                    Bep2(it)
                }
                "spl" -> chunks.getOrNull(1)?.let {
                    Spl(it)
                }
                "unsupported" -> chunks.getOrNull(1)?.let {
                    Unsupported(it, chunks.getOrNull(2))
                }
                else -> null
            }

        }

    }

}
