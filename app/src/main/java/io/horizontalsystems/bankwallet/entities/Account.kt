package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.hdwalletkit.HDKey
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.horizontalsystems.hdwalletkit.Utils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.math.BigInteger
import kotlin.experimental.and

@Parcelize
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val origin: AccountOrigin,
    val isBackedUp: Boolean = false
) : Parcelable {

    @IgnoredOnParcel
    val isWatchAccount: Boolean
        get() = when (this.type) {
            is AccountType.EvmAddress -> true
            is AccountType.HdExtendedKey -> this.type.hdExtendedKey.info.isPublic
            else -> false
        }

    override fun equals(other: Any?): Boolean {
        if (other is Account) {
            return id == other.id
        }

        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@Parcelize
open class AccountType : Parcelable {
    @Parcelize
    data class EvmAddress(val address: String) : AccountType()

    @Parcelize
    data class Mnemonic(val words: List<String>, val passphrase: String) : AccountType() {
        @IgnoredOnParcel
        val seed by lazy { Mnemonic().toSeed(words, passphrase) }

        override fun equals(other: Any?): Boolean {
            return other is Mnemonic
                    && words.toTypedArray().contentEquals(other.words.toTypedArray())
                    && passphrase == other.passphrase
        }

        override fun hashCode(): Int {
            return words.toTypedArray().contentHashCode() + passphrase.hashCode()
        }
    }

    @Parcelize
    data class EvmPrivateKey(val key: ByteArray) : AccountType() {
        override fun equals(other: Any?): Boolean {
            return other is EvmPrivateKey && key.contentEquals(other.key)
        }

        override fun hashCode(): Int {
            return key.contentHashCode()
        }
    }

    @Parcelize
    data class HdExtendedKey(val keySerialized: String) : AccountType() {
        val hdExtendedKey: HDExtendedKey
            get() = HDExtendedKey(keySerialized)

        override fun equals(other: Any?): Boolean {
            return other is HdExtendedKey && keySerialized.contentEquals(other.keySerialized)
        }

        override fun hashCode(): Int {
            return keySerialized.hashCode()
        }
    }

    @Parcelize
    enum class Derivation(val value: String) : Parcelable {
        bip44("bip44"),
        bip49("bip49"),
        bip84("bip84");

        companion object {
            private val map = values().associateBy(Derivation::value)

            fun fromString(value: String?): Derivation? = map[value]
        }
    }

    val description: String
        get() = when (this) {
            is Mnemonic -> {
                val count = words.size

                if (passphrase.isNotBlank()) {
                    Translator.getString(R.string.ManageAccount_NWordsWithPassphrase, count)
                } else {
                    Translator.getString(R.string.ManageAccount_NWords, count)
                }
            }
            is EvmAddress -> this.address.shorten()
            is EvmPrivateKey -> "EVM Private Key"
            is HdExtendedKey -> {
                when (this.hdExtendedKey.derivedType) {
                    HDExtendedKey.DerivedType.Master -> "BIP32 Root Key"
                    HDExtendedKey.DerivedType.Account -> {
                        if (hdExtendedKey.info.isPublic) {
                            "Account xPubKey"
                        } else {
                            "Account xPrivKey"
                        }
                    }
                    else -> ""
                }
            }
            else -> ""
        }

    val supportedDerivations: List<Derivation>
        get() = when (this) {
            is Mnemonic -> {
                listOf(Derivation.bip44, Derivation.bip49, Derivation.bip84)
            }
            is HdExtendedKey -> {
                listOf(this.hdExtendedKey.info.purpose.derivation)
            }
            else -> emptyList()
        }
}

val HDWallet.Purpose.derivation: AccountType.Derivation
    get() = when (this) {
        HDWallet.Purpose.BIP44 -> AccountType.Derivation.bip44
        HDWallet.Purpose.BIP49 -> AccountType.Derivation.bip49
        HDWallet.Purpose.BIP84 -> AccountType.Derivation.bip84
    }

val AccountType.Derivation.addressType: String
    get() = when (this) {
        AccountType.Derivation.bip44 -> "Legacy"
        AccountType.Derivation.bip49 -> "SegWit"
        AccountType.Derivation.bip84 -> "Native SegWit"
    }

val AccountType.Derivation.rawName: String
    get() = when (this) {
        AccountType.Derivation.bip44 -> "BIP 44"
        AccountType.Derivation.bip49 -> "BIP 49"
        AccountType.Derivation.bip84 -> "BIP 84"
    }

val AccountType.Derivation.title: String
    get() = when (this) {
        AccountType.Derivation.bip44 -> Translator.getString(R.string.CoinOption_bip44_Title)
        AccountType.Derivation.bip49 -> Translator.getString(R.string.CoinOption_bip49_Title)
        AccountType.Derivation.bip84 -> Translator.getString(R.string.CoinOption_bip84_Title)
    }

val AccountType.Derivation.description: String
    get() = when (this) {
        AccountType.Derivation.bip44 -> rawName
        AccountType.Derivation.bip84,
        AccountType.Derivation.bip49 -> "$rawName - $addressType"
    }

@Parcelize
enum class AccountOrigin(val value: String) : Parcelable {
    Created("Created"),
    Restored("Restored");
}

//todo Remove this stub part

class HDExtendedKey(
    val key: HDKey,
    private val version: HDExtendedKeyVersion
) {

    constructor(serialized: String) : this(key(serialized), version(serialized))

    val derivedType: DerivedType
        get() = DerivedType.initFrom(key.depth.toByte())

    val info: KeyInfo
        get() = KeyInfo(
            purpose = version.purpose,
            coinType = version.extendedKeyCoinType,
            derivedType = DerivedType.initFrom(key.depth.toByte()),
            isPublic = version.isPublic
        )

    fun serializeBase58(): String {
        return ""//key.serializePubB58(version.value)
    }

    companion object {
        private const val length = 82

        private fun key(serialized: String): HDKey {
            val version = version(serialized)

            val data = "".toByteArray()//Base58.decode(serialized)
            if (data.size != length) {
                throw ParsingError.WrongKeyLength
            }

            val depth = data[4] and 0xff.toByte()
            val derivedType = DerivedType.initFrom(depth)
            if (derivedType == DerivedType.Bip32) {
                throw ParsingError.WrongDerivedType
            }

            validateChecksum(data)

            val bytes: ByteArray = data.copyOfRange(0, data.size - 4)
            val chainCode: ByteArray = bytes.copyOfRange(13, 13 + 32)
            val pubOrPrv: ByteArray = bytes.copyOfRange(13 + 32, bytes.size)

            return HDKey(pubOrPrv, chainCode, null, depth.toInt(), true)
        }

        @Throws
        fun version(serialized: String): HDExtendedKeyVersion {
            val prefix = serialized.take(4)
            return HDExtendedKeyVersion.initFrom(prefix) ?: throw ParsingError.WrongVersion
        }

        @Throws
        fun validate(extendedKey: ByteArray, isPublic: Boolean) {
            if (extendedKey.size != length) {
                throw ParsingError.WrongKeyLength
            }

            val version = HDExtendedKeyVersion.initFrom(extendedKey.sliceArray(0..4))
            if (version == null || version.isPublic != isPublic) {
                throw ParsingError.WrongVersion
            }

            validateChecksum(extendedKey)
        }

        fun validateChecksum(extendedKey: ByteArray) {
            val bytes = extendedKey.copyOfRange(0, extendedKey.size - 4)
            val checksum = extendedKey.copyOfRange(extendedKey.size - 4, extendedKey.size)
            val hash = Utils.doubleDigest(bytes).copyOfRange(0, 4)
            if (!hash.contentEquals(checksum)) {
                throw ParsingError.InvalidChecksum
            }
        }
    }

    enum class DerivedType {
        Bip32,
        Master,
        Account;

        companion object {
            //master key depth == 0, account depth = "m/purpose'/coin_type'/account'" = 3, all others is custom
            fun initFrom(depth: Byte) =
                when (depth.toInt()) {
                    0 -> Master
                    3 -> Account
                    else -> Bip32
                }
        }
    }

    data class KeyInfo(
        val purpose: HDWallet.Purpose,
        val coinType: ExtendedKeyCoinType,
        val derivedType: DerivedType,
        val isPublic: Boolean
    )

    sealed class ParsingError : Throwable() {
        object WrongVersion : ParsingError()
        object WrongKeyLength : ParsingError()
        object WrongDerivedType : ParsingError()
        object InvalidChecksum : ParsingError()
    }
}


enum class HDExtendedKeyVersion(
    val value: Int,
    val base58Prefix: String,
    val purpose: HDWallet.Purpose,
    val extendedKeyCoinType: ExtendedKeyCoinType = ExtendedKeyCoinType.Bitcoin,
    val isPublic: Boolean = false
) {

    // bip44
    xprv(0x0488ade4, "xprv", HDWallet.Purpose.BIP44),
    xpub(0x0488b21e, "xpub", HDWallet.Purpose.BIP44, isPublic = true),

    //bip49
    yprv(0x049d7878, "yprv", HDWallet.Purpose.BIP49),
    ypub(0x049d7cb2, "ypub", HDWallet.Purpose.BIP49, isPublic = true),

    //bip84
    zprv(0x04b2430c, "zprv", HDWallet.Purpose.BIP84),
    zpub(0x04b24746, "zpub", HDWallet.Purpose.BIP84, isPublic = true),


    // litecoin bip44
    Ltpv(0x019d9cfe, "Ltpv", HDWallet.Purpose.BIP44, ExtendedKeyCoinType.Litecoin),
    Ltub(0x019da462, "Ltub", HDWallet.Purpose.BIP44, ExtendedKeyCoinType.Litecoin, isPublic = true),


    // litecoin bip49
    Mtpv(0x01b26792, "Mtpv", HDWallet.Purpose.BIP49, ExtendedKeyCoinType.Litecoin),
    Mtub(0x01b26ef6, "Mtub", HDWallet.Purpose.BIP49, ExtendedKeyCoinType.Litecoin, isPublic = true);


    companion object {
        fun initFrom(
            purpose: HDWallet.Purpose,
            coinType: ExtendedKeyCoinType,
            isPrivate: Boolean
        ): HDExtendedKeyVersion? {
            return when (purpose) {
                HDWallet.Purpose.BIP44 -> {
                    when (coinType) {
                        ExtendedKeyCoinType.Bitcoin -> if (isPrivate) xprv else xpub
                        ExtendedKeyCoinType.Litecoin -> if (isPrivate) Ltpv else Ltub
                    }
                }

                HDWallet.Purpose.BIP49 -> {
                    when (coinType) {
                        ExtendedKeyCoinType.Bitcoin -> if (isPrivate) yprv else ypub
                        ExtendedKeyCoinType.Litecoin -> if (isPrivate) Mtpv else Mtub
                    }
                }

                HDWallet.Purpose.BIP84 -> {
                    when (coinType) {
                        ExtendedKeyCoinType.Bitcoin -> if (isPrivate) zprv else zpub
                        ExtendedKeyCoinType.Litecoin -> null
                    }
                }
            }
        }

        fun initFrom(prefix: String): HDExtendedKeyVersion? =
            values().firstOrNull { it.base58Prefix == prefix }

        fun initFrom(version: ByteArray): HDExtendedKeyVersion? =
            values().firstOrNull { it.value == BigInteger(version).toInt() }
    }
}

enum class ExtendedKeyCoinType {
    Bitcoin, Litecoin
}
