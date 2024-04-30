package io.horizontalsystems.marketkit.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class TokenType : Parcelable {

    enum class Derivation {
        Bip44,
        Bip49,
        Bip84,
        Bip86,
    }

    enum class AddressType {
        Type0,
        Type145,
    }

    @Parcelize
    object Native : TokenType()

    @Parcelize
    data class Derived(val derivation: Derivation) : TokenType()

    @Parcelize
    data class AddressTyped(val type: AddressType) : TokenType()

    @Parcelize
    data class Eip20(val address: String) : TokenType()

    @Parcelize
    data class Bep2(val symbol: String) : TokenType()

    @Parcelize
    data class Spl(val address: String) : TokenType()

    @Parcelize
    data class Unsupported(val type: String, val reference: String) : TokenType()

    val id: String
        get() {
            val parts = when (this) {
                Native -> listOf("native")
                is Eip20 -> listOf("eip20", address)
                is Bep2 -> listOf("bep2", symbol)
                is Spl -> listOf("spl", address)
                is AddressTyped -> listOf("address_type", type.name.lowercase())
                is Derived -> listOf("derived", derivation.name.lowercase())
                is Unsupported -> if (reference.isNotBlank()) {
                    listOf("unsupported", type, reference)
                } else {
                    listOf("unsupported", type)
                }
            }
            return parts.joinToString(":")
        }

    val values: Value
        get() = when (this) {
            is Native -> Value("native", "")
            is Eip20 -> Value("eip20", address)
            is Bep2 -> Value("bep2", symbol)
            is Spl -> Value("spl", address)
            is AddressTyped -> Value("address_type", type.name)
            is Derived -> Value("derived", derivation.name)
            is Unsupported -> Value(type, reference)
        }

    data class Value(
        val type: String,
        val reference: String
    )

    companion object {

        fun fromType(type: String, reference: String = ""): TokenType {
            when (type) {
                "native" -> return Native

                "eip20" -> {
                    if (reference.isNotBlank()) {
                        return Eip20(reference)
                    }
                }

                "bep2" -> {
                    if (reference.isNotBlank()) {
                        return Bep2(reference)
                    }
                }

                "spl" -> {
                    if (reference.isNotBlank()) {
                        return Spl(reference)
                    }
                }

                "address_type" -> {
                    if (reference.isNotBlank()) {
                        try {
                            return AddressTyped(AddressType.valueOf(reference.lowercase().replaceFirstChar(Char::uppercase)))
                        } catch (e: IllegalArgumentException) {
                        }
                    }
                }

                "derived" -> {
                    if (reference.isNotBlank()) {
                        try {
                            return Derived(Derivation.valueOf(reference.lowercase().replaceFirstChar(Char::uppercase)))
                        } catch (e: IllegalArgumentException) {
                        }
                    }
                }
            }

            return Unsupported(type, reference)
        }

        fun fromId(id: String): TokenType? {
            val chunks = id.split(":")
            val type = chunks[0]
            val reference = chunks.getOrNull(1) ?: ""

            return fromType(type, reference)
        }
    }

}
