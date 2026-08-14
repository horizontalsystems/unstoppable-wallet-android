package io.horizontalsystems.walletkit.modules.privatesend

import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import io.horizontalsystems.walletkit.modules.multiswap.providers.memoDelivery
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token

/**
 * Builds the deposit transfer for a committed private send order: exactly
 * [PrivateSendOrder.depositAmount] to [PrivateSendOrder.depositAddress], carrying the order's
 * attachment only when the chain actually delivers it to the provider.
 */
object PrivateSendDepositBuilder {

    fun build(order: PrivateSendOrder): SendTransactionData {
        val token = order.request.token
        val memo = deliverableMemo(order.attachment, token.blockchainType)

        // EVM needs the kit for ERC20 transfer calldata, so its plugin builds the data.
        // (No EVM path carries a memo, and the gate above already rejected any attachment.)
        ChainRegistry[token.blockchainType]?.depositTransferData(token, order.depositAmount, order.depositAddress)?.let {
            return it
        }

        val address = order.depositAddress
        val amount = order.depositAmount

        return when (token.blockchainType) {
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.ECash,
            BlockchainType.Litecoin,
            BlockchainType.Dash,
                -> SendTransactionData.Btc(
                address = address,
                memo = memo,
                amount = amount,
                recommendedGasRate = null,
                minimumSendAmount = null,
                changeToFirstInput = false,
                utxoFilters = UtxoFilters(),
            )

            BlockchainType.Tron -> {
                // A simple Tron send cannot carry the provider's crediting identifier.
                if (memo != null) throw PrivateSendError.AttachmentUnsupported
                SendTransactionData.Tron.Simple(address, amount)
            }

            BlockchainType.Solana -> {
                if (memo != null) throw PrivateSendError.AttachmentUnsupported
                SendTransactionData.Solana.Simple(address, amount)
            }

            BlockchainType.Stellar -> SendTransactionData.Stellar.Regular(
                address = address,
                memo = memo.orEmpty(),
                amount = amount,
            )

            BlockchainType.Ton -> SendTransactionData.Ton.Regular(
                address = address,
                amount = amount,
                memo = memo,
            )

            BlockchainType.Zcash -> SendTransactionData.Zcash.Regular(
                address = address,
                amount = amount,
                memo = memo.orEmpty(),
            )

            BlockchainType.Monero -> SendTransactionData.Monero(
                address = address,
                amount = amount,
                memo = memo,
            )

            BlockchainType.Zano -> SendTransactionData.Zano(
                address = address,
                amount = amount,
                memo = memo,
            )

            BlockchainType.Thorchain,
            BlockchainType.Mayachain,
                -> SendTransactionData.Thorchain.Send(
                address = address,
                amount = amount,
                memo = memo.orEmpty(),
            )

            else -> throw PrivateSendError.CommitFailed()
        }
    }

    /**
     * Only a text attachment carried by a plain transfer this app builds — and only on a chain
     * where that memo actually reaches the deposit-address owner — may proceed. Anything else
     * must fail the send rather than be dropped: the provider matches the incoming deposit to
     * the order by this identifier, and a deposit it cannot match is typically unrecoverable.
     */
    private fun deliverableMemo(
        attachment: UnstoppableAPI.Response.Attachment?,
        blockchainType: BlockchainType,
    ): String? {
        attachment ?: return null

        // No private-send chain carries a destination tag as a separate transaction field, and
        // an unknown attachment kind cannot be carried at all.
        if (attachment.type != "text") throw PrivateSendError.AttachmentUnsupported

        if (!blockchainType.memoDelivery.deliversAttachment) {
            throw PrivateSendError.AttachmentUnsupported
        }

        return attachment.value
    }
}
