package bitcoin.wallet.kit.hdwallet

import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.walllet.kit.hdwallet.HDKey

class HDWallet(private val seed: ByteArray, private val networkParams: NetworkParameters) {

    private var hdKeychain: HDKeychain = HDKeychain(seed, networkParams)


    // m / purpose' / coin_type' / account' / change / address_index
    //
    // Purpose is a constant set to 44' (or 0x8000002C) following the BIP43 recommendation.
    // It indicates that the subtree of this node is used according to this specification.
    // Hardened derivation is used at this level.
    private var purpose: Int = 44

    // One master node (seed) can be used for unlimited number of independent cryptocoins such as Bitcoin, Litecoin or Namecoin. However, sharing the same space for various cryptocoins has some disadvantages.
    // This level creates a separate subtree for every cryptocoin, avoiding reusing addresses across cryptocoins and improving privacy issues.
    // Coin type is a constant, set for each cryptocoin. Cryptocoin developers may ask for registering unused number for their project.
    // The list of already allocated coin types is in the chapter "Registered coin types" below.
    // Hardened derivation is used at this level.
    // network.name == MainNet().name ? 0 : 1
    // private var coinType: Int = 0

    // This level splits the key space into independent user identities, so the wallet never mixes the coins across different accounts.
    // Users can use these accounts to organize the funds in the same fashion as bank accounts; for donation purposes (where all addresses are considered public), for saving purposes, for common expenses etc.
    // Accounts are numbered from index 0 in sequentially increasing manner. This number is used as child index in BIP32 derivation.
    // Hardened derivation is used at this level.
    // Software should prevent a creation of an account if a previous account does not have a transaction history (meaning none of its addresses have been used before).
    // Software needs to discover all used accounts after importing the seed from an external source. Such an algorithm is described in "Account discovery" chapter.
    private var account: Int = 0


    fun receiveAddress(index: Int): PublicKey {
        return PublicKey(index = index, external = true, key = privateKey(index = index, chain = 0), network = networkParams)
    }

    fun changeAddress(index: Int): PublicKey {
        return PublicKey(index = index, external = false, key = privateKey(index = index, chain = 1), network = networkParams)
    }

    private fun privateKey(index: Int, chain: Int): HDKey {
        return privateKey(path = "m/$purpose'/${networkParams.coinType}'/$account'/$chain/$index")
    }

    private fun privateKey(path: String): HDKey {
        return hdKeychain.getKeyByPath(path)
    }

}
