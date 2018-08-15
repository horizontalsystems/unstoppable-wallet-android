/*
 * Copyright 2016 Ronald W Hoffman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitcoin.walllet.kit.common.hdwallet;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Manage the HD key hierarchy (BIP 32)
 *<pre>
 *                      +----------------- Root ------------------+
 *                      |                  (m)                    |
 *                      |                                         |
 *             +----- Account-0 -----+     ...           +----- Account-n -----+
 *             |       (m/0)         |                   |      (m/n)          |
 *             |                     |                   |                     |
 *      +--- Chain-0 ---+ ... +--- Chain-n ---+     +--- Chain-0 ---+ ... +--- Chain-n ---+
 *      |    (m/0/0)    |     |    (m/0/n)    |     |    (m/n/0)    |     |    (m/n/n)    |
 *      |               |     |               |     |               |     |               |
 *    key-0   ...     key-n key-0   ...     key-n key-0 ...       key-n key-0   ...     key-n
 *</pre>
 * <p>An account key is a child of the root key.  Each account has one or more chain
 * keys.  Each chain key has one or more application keys.  Each key contains a
 * list of child keys (if any).
 *
 * <p>A hardened key has the high-order bit set.  Hardened keys are maintained separately from
 * non-hardened keys.  Thus, the key hierarch can potentially have two nodes for a given
 * child number.
 */
public class HDHierarchy {

    /** Root key */
    private final HDKey rootKey;

    /** Root node */
    private final Node rootNode;

    /**
     * Create a new hierarchy
     *
     * @param   rootKey             Root key
     */
    public HDHierarchy(HDKey rootKey) {
        this.rootKey = rootKey;
        this.rootNode = new Node(rootKey);
        rootKey.setNode(this.rootNode);
    }

    /**
     * Return the root key
     *
     * @return                      Root key
     */
    public HDKey getRootKey() {
        return rootKey;
    }

    /**
     * <p>Derive a child key.
     *
     * <p>An existing key will be returned if it is found in the key hierarchy.  Hardened keys
     * are stored separately, so it is possible to have both hardened and non-hardened keys
     * stored in the deterministic hierarchy.
     *
     * <p>The parent must have a private key in order to derive a private/public key pair.
     * If the parent does not have a private key, only the public key can be derived.
     * In addition, a hardened key cannot be derived from a public key since the algorithm requires
     * the parent private key.
     *
     * <p>It is possible for key derivation to fail for a child number because the generated
     * key is not valid.  If this happens, the application should generate a key using
     * a different child number.
     *
     * @param   parent                  Parent key
     * @param   childNumber             Child number
     * @param   hardened                TRUE to harden the child
     * @return                          Derived key
     * @throws  HDDerivationException   Unable to derive the key
     */
    public HDKey deriveChildKey(HDKey parent, int childNumber, boolean hardened)
                                        throws HDDerivationException {
        if ((childNumber&HDKey.HARDENED_FLAG) != 0)
            throw new IllegalArgumentException("Hardened flag must not be set in child number");
        Node parentNode = parent.getNode();
        //
        // Return an existing key
        //
        if (parentNode != null) {
            HDKey childKey = parentNode.getChildKey(hardened ? (childNumber|HDKey.HARDENED_FLAG) : childNumber);
            if (childKey != null) {
                return childKey;
            }
        }
        //
        // Derive the child key
        //
        HDKey childKey = HDKeyDerivation.deriveChildKey(parent, childNumber, hardened);
        //
        // Add the derived key as a child of the parent key
        //
        if (parentNode == null) {
            parentNode = new Node(parent);
            parent.setNode(parentNode);
        }
        parentNode.addChildKey(childKey);
        return childKey;
    }

    /**
     * Hierarchy node
     */
    public class Node {

        /** Key for this node */
        private final HDKey key;

        /** Children associated with this node */
        private final Map<Integer, HDKey> children = new HashMap<>();

        /**
         *
         * Create a new node
         *
         * @param   key                 Key for this node
         */
        Node(HDKey key) {
            this.key = key;
        }

        /**
         * Add a child key
         *
         * @param   childKey            Child key to add
         */
        void addChildKey(HDKey childKey) {
            int hashKey = childKey.isHardened() ? (childKey.getChildNumber()|HDKey.HARDENED_FLAG) : childKey.getChildNumber();
            children.put(hashKey, childKey);
        }

        /**
         * Return the node key
         *
         * @return                      Node key
         */
        public HDKey getKey() {
            return key;
        }

        /**
         * Return the child key associated with the specified child number.  The high-order
         * bit in the child number must be set to retrieve a hardened key.
         *
         * @param   childNumber         Child number
         * @return                      Child key or null if the key doesn't exist
         */
        public HDKey getChildKey(int childNumber) {
            return children.get(childNumber);
        }
    }
}
