package io.horizontalsystems.walletkit.modules.privatesend

import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.modules.multiswap.providers.UnstoppableAPI
import io.horizontalsystems.walletkit.modules.multiswap.providers.XrpDestinationTag
import io.horizontalsystems.walletkit.modules.multiswap.providers.memoDelivery
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.privatesend.PrivateSendDepositBuilder.deliverableDestinationTag
import io.horizontalsystems.walletkit.modules.privatesend.PrivateSendDepositBuilder.deliverableMemo

/**
 * Builds the deposit transfer for a committed private send order: exactly
 * [PrivateSendOrder.depositAmount] to [PrivateSendOrder.depositAddress], carrying the order's
 * attachment only when the chain actually delivers it to the provider.
 */
object PrivateSendDepositBuilder {

    fun build(order: PrivateSendOrder, btcParams: PrivateSendBtcParams? = null): SendTransactionData {
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
                // The send screen's settings, so a private send honours the user's coin
                // control, fee rate, sorting and RBF choice. No timelock: the deposit must
                // be spendable by the provider immediately.
                recommendedGasRate = btcParams?.feeRate,
                minimumSendAmount = null,
                changeToFirstInput = false,
                utxoFilters = UtxoFilters(),
                unspentOutputs = btcParams?.unspentOutputs,
                transactionSorting = btcParams?.transactionSorting,
                rbfEnabled = btcParams?.rbfEnabled ?: false,
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

            // XRP carries the identifier in the Payment's DestinationTag field, not a memo, so
            // a `text` attachment has nowhere to ride here and must refuse — see
            // [deliverableDestinationTag].
            BlockchainType.Xrp -> SendTransactionData.Xrp(
                address = address,
                amount = amount,
                destinationTag = deliverableDestinationTag(order.attachment),
            )

            // No Zcash/Monero/Zano branches: those chains' own transactions already hide the
            // sender, so PrivateSendManager excludes them outright — see its privateChains.

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
     * Only an attachment carried by a plain transfer this app builds — and only on a chain
     * where that memo actually reaches the deposit-address owner — may proceed. Anything else
     * must fail the send rather than be dropped: the provider matches the incoming deposit to
     * the order by this identifier, and a deposit it cannot match is typically unrecoverable.
     *
     * Both `text` and `destination_tag` ride the memo field, exactly as the swap deposit path
     * treats them (see Execution.resolvedMemo): every memo-carrying chain built here puts a
     * numeric tag (e.g. a Stellar memo-id) in the same memo slot. XRP is the exception — its
     * tag is a separate transaction field, read by [deliverableDestinationTag] instead — and
     * an unknown attachment kind still refuses.
     *
     * Also called by PrivateSendManager.commit right after the order is committed, so an
     * undeliverable attachment surfaces once as an authored commit error instead of failing
     * later on every re-entry into the deposit build.
     */
    fun deliverableMemo(
        attachment: UnstoppableAPI.Response.Attachment?,
        blockchainType: BlockchainType,
    ): String? {
        attachment ?: return null

        if (attachment.type != "text" && attachment.type != "destination_tag") {
            throw PrivateSendError.AttachmentUnsupported
        }

        if (!blockchainType.memoDelivery.deliversAttachment) {
            throw PrivateSendError.AttachmentUnsupported
        }

        // A blank identifier is as unmatchable as a dropped one.
        if (attachment.value.isBlank()) {
            throw PrivateSendError.AttachmentUnsupported
        }

        // XRP's identifier is a separate transaction field, so deliverability there is a
        // narrower question than "does a memo reach the owner". Asking it here keeps the
        // commit-time gate honest for XRP instead of letting it fail in the build.
        if (blockchainType == BlockchainType.Xrp) {
            deliverableDestinationTag(attachment)
        }

        return attachment.value
    }

    /**
     * The XRPL destination tag for an order's attachment, or null when it carries none.
     *
     * Deliberately stricter than [deliverableMemo]: the tag is a 32-bit unsigned field of the
     * Payment, so a `text` attachment cannot be delivered there at all (XRPL memos are not what
     * providers read), and a value outside the field's range would be truncated into somebody
     * else's order. Both refuse the send instead.
     */
    private fun deliverableDestinationTag(attachment: UnstoppableAPI.Response.Attachment?): Long? {
        attachment ?: return null

        if (attachment.type != "destination_tag") throw PrivateSendError.AttachmentUnsupported

        return XrpDestinationTag.parse(attachment.value) ?: throw PrivateSendError.AttachmentUnsupported
    }
}
