package io.horizontalsystems.walletkit.modules.addtoken

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.xrpkit.XrpKit
import io.horizontalsystems.walletkit.core.customCoinUid
import io.horizontalsystems.walletkit.core.factories.XrpTransactionConverter

/**
 * Adds an XRPL issued currency by reference. Accepted forms: `CODE.issuer`, `CODE-issuer`,
 * `issuer.CODE`, or an xrpscan token URL. CODE may be the 3-character code, the 40-hex ledger
 * form, or a longer human name such as `RLUSD`, which is converted to its hex form.
 */
class AddXrpTokenBlockchainService(private val blockchain: Blockchain) : AddTokenModule.IAddTokenBlockchainService {

    private class Parsed(val currency: String, val issuer: String)

    override fun isValid(reference: String) = parse(reference) != null

    override fun tokenQuery(reference: String): TokenQuery {
        val parsed = parse(reference) ?: throw IllegalArgumentException("Invalid XRPL token reference")
        return TokenQuery(BlockchainType.Xrp, TokenType.XrpAsset(parsed.currency, parsed.issuer))
    }

    override suspend fun token(reference: String): Token {
        val parsed = parse(reference) ?: throw IllegalArgumentException("Invalid XRPL token reference")
        val tokenQuery = tokenQuery(reference)
        val code = XrpKit.displayCurrencyCode(parsed.currency)
        return Token(
            coin = Coin(
                uid = tokenQuery.customCoinUid,
                name = code,
                code = code,
                image = null,
            ),
            blockchain = blockchain,
            type = tokenQuery.tokenType,
            decimals = XrpTransactionConverter.ISSUED_TOKEN_DECIMALS,
        )
    }

    private fun parse(input: String): Parsed? {
        var text = input.trim()
        if (text.isEmpty()) return null

        // https://xrpscan.com/token/RLUSD.rMxCK... or https://bithomp.com/token/issuer/CODE
        if (text.startsWith("http")) {
            text = text.substringAfter("/token/").trimEnd('/')
        }

        val parts = text.split('.', '-', '/', ':').filter { it.isNotEmpty() }
        if (parts.size != 2) return null

        val (a, b) = parts
        val (rawCode, issuer) = when {
            XrpKit.isValidAddress(b) && !b.startsWith("X") -> a to b
            XrpKit.isValidAddress(a) && !a.startsWith("X") -> b to a
            else -> return null
        }
        val currency = toLedgerCurrency(rawCode) ?: return null
        return Parsed(currency, issuer)
    }

    /** 3-character and 40-hex codes are kept; a 4..20 character ASCII name becomes its hex form. */
    private fun toLedgerCurrency(code: String): String? {
        if (code.equals("XRP", ignoreCase = true)) return null
        if (XrpKit.isValidCurrencyCode(code)) {
            return if (code.length == 40) code.uppercase() else code
        }
        if (code.length in 4..20 && code.all { it.code in 0x21..0x7E }) {
            val hex = code.toByteArray(Charsets.US_ASCII).joinToString("") { "%02X".format(it) }
            return hex.padEnd(40, '0')
        }
        return null
    }
}
