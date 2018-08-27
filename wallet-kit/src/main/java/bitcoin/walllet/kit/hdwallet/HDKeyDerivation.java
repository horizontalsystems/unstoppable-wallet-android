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
package bitcoin.walllet.kit.hdwallet;

import org.spongycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;

import bitcoin.walllet.kit.exceptions.HDDerivationException;

/**
 * Hierarchical Deterministic key derivation (BIP 32)
 */
public class HDKeyDerivation {

    /**
     * Generate a root key from the given seed.  The seed must be at least 128 bits.
     *
     * @param   seed                    HD seed
     * @return                          Root key
     * @throws  HDDerivationException   Generated master key is invalid
     */
    public static HDKey createRootKey(byte[] seed) throws HDDerivationException {
        if (seed.length < 16)
            throw new IllegalArgumentException("Seed must be at least 128 bits");
        //
        // From BIP 32:
        // - Generate a seed byte sequence S of a chosen length (between 128 and 512 bits)
        // - Calculate I = HMAC-SHA512(Key = "Bitcoin seed", Data = S)
        // - Split I into two 32-byte sequences, IL and IR.
        // - Use parse256(IL) as master secret key, and IR as master chain code.
        //   In case IL is 0 or ≥n, the master key is invalid.
        //
        byte[] i = Utils.hmacSha512("Bitcoin seed".getBytes(), seed);
        byte[] il = Arrays.copyOfRange(i, 0, 32);
        byte[] ir = Arrays.copyOfRange(i, 32, 64);
        BigInteger privKey = new BigInteger(1, il);
        if (privKey.signum() == 0)
            throw new HDDerivationException("Generated master private key is zero");
        if (privKey.compareTo(ECKey.ecParams.getN()) >= 0)
            throw new HDDerivationException("Generated master private key is not less than N");
        return new HDKey(privKey, ir, null, 0, false);
    }

    /**
     * <p>Derive a child key from the specified parent.
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
     * @param   hardened                TRUE to create a hardened key
     * @return                          Derived key
     * @throws  HDDerivationException   Unable to derive key
     */
    public static HDKey deriveChildKey(HDKey parent, int childNumber, boolean hardened)
                                        throws HDDerivationException {
        HDKey derivedKey;
        if ((childNumber&HDKey.HARDENED_FLAG) != 0)
            throw new IllegalArgumentException("Hardened flag must not be set in child number");
        if (parent.getPrivKey() == null) {
            if (hardened)
                throw new IllegalStateException("Hardened key requires parent private key");
            derivedKey = derivePublicKey(parent, childNumber);
        } else {
            derivedKey = derivePrivateKey(parent, childNumber, hardened);
        }
        return derivedKey;
    }

    /**
     * Derive a child key from a private key
     *
     * @param   parent                  Parent key
     * @param   childNumber             Child number
     * @param   hardened                TRUE to create a hardened key
     * @return                          Derived key
     * @throws  HDDerivationException   Unable to derive key
     */
    private static HDKey derivePrivateKey(HDKey parent, int childNumber, boolean hardened)
                                        throws HDDerivationException {
        byte[] parentPubKey = parent.getPubKey();
        if (parentPubKey.length != 33)
            throw new IllegalStateException("Parent public key is not 33 bytes");
        //
        // From BIP 32:
        // - Check whether i ≥ 231 (whether the child is a hardened key).
        // - If so (hardened child): let I = HMAC-SHA512(Key = cpar, Data = 0x00 || ser256(kpar) || ser32(i)).
        //   (Note: The 0x00 pads the private key to make it 33 bytes long.)
        // - If not (normal child): let I = HMAC-SHA512(Key = cpar, Data = serP(point(kpar)) || ser32(i)).
        // - Split I into two 32-byte sequences, IL and IR.
        // - The returned child key ki is parse256(IL) + kpar (mod n).
        // - The returned chain code ci is IR.
        //
        // In case parse256(IL) ≥ n or ki = 0, the resulting key is invalid,
        // and one should proceed with the next value for i.
        //
        ByteBuffer dataBuffer = ByteBuffer.allocate(37);
        if (hardened) {
            dataBuffer.put(parent.getPaddedPrivKeyBytes())
                      .putInt(childNumber|HDKey.HARDENED_FLAG);
        } else {
            dataBuffer.put(parentPubKey)
                      .putInt(childNumber);
        }
        byte[] i = Utils.hmacSha512(parent.getChainCode(), dataBuffer.array());
        byte[] il = Arrays.copyOfRange(i, 0, 32);
        byte[] ir = Arrays.copyOfRange(i, 32, 64);
        BigInteger ilInt = new BigInteger(1, il);
        if (ilInt.compareTo(ECKey.ecParams.getN()) >= 0)
            throw new HDDerivationException("Derived private key is not less than N");
        BigInteger ki = parent.getPrivKey().add(ilInt).mod(ECKey.ecParams.getN());
        if (ki.signum() == 0)
            throw new HDDerivationException("Derived private key is zero");
        return new HDKey(ki, ir, parent, childNumber, hardened);
    }

    /**
     * Derive a child key from a public key
     *
     * @param   parent                  Parent key
     * @param   childNumber             Child number
     * @return                          Derived key
     * @throws  HDDerivationException   Unable to derive key
     */
    private static HDKey derivePublicKey(HDKey parent, int childNumber)
                                        throws HDDerivationException {
        //
        // - If not (normal child): let I = HMAC-SHA512(Key = cpar, Data = serP(Kpar) || ser32(i)).
        // - Split I into two 32-byte sequences, IL and IR.
        // - The returned child key Ki is point(parse256(IL)) + Kpar.
        // - The returned chain code ci is IR.
        //
        // In case parse256(IL) ≥ n or Ki is the point at infinity, the resulting key is invalid,
        // and one should proceed with the next value for i.
        //
        ByteBuffer dataBuffer = ByteBuffer.allocate(37);
        dataBuffer.put(parent.getPubKey()).putInt(childNumber);
        byte[] i = Utils.hmacSha512(parent.getChainCode(), dataBuffer.array());
        byte[] il = Arrays.copyOfRange(i, 0, 32);
        byte[] ir = Arrays.copyOfRange(i, 32, 64);
        BigInteger ilInt = new BigInteger(1, il);
        if (ilInt.compareTo(ECKey.ecParams.getN()) >= 0)
            throw new HDDerivationException("Derived private key is not less than N");
        ECPoint pubKeyPoint = ECKey.ecParams.getCurve().decodePoint(parent.getPubKey());
        ECPoint Ki = ECKey.pubKeyPointFromPrivKey(ilInt).add(pubKeyPoint);
        if (Ki.equals(ECKey.ecParams.getCurve().getInfinity()))
            throw new HDDerivationException("Derived public key equals infinity");
        return new HDKey(Ki.getEncoded(true), ir, parent, childNumber, false);
    }

}
