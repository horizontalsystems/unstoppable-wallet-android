/**
 * Copyright 2011 Google Inc.
 * Copyright 2013-2016 Ronald W Hoffman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bitcoin.walllet.kit.common.hdwallet;

import org.spongycastle.asn1.x9.X9ECParameters;
import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.ec.CustomNamedCurves;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECKeyGenerationParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.math.ec.ECFieldElement;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.math.ec.custom.sec.SecP256K1Curve;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * ECKey supports elliptic curve cryptographic operations using a public/private
 * key pair.  A private key is required to create a signature and a public key is
 * required to verify a signature.  The private key is always encrypted using AES
 * when it is serialized for storage on external media.
 */
public class ECKey {

    /** Half-curve order for generating canonical S */
    public static final BigInteger HALF_CURVE_ORDER;

    /** Elliptic curve parameters (secp256k1 curve) */
    public static final ECDomainParameters ecParams;
    static {
        X9ECParameters params = CustomNamedCurves.getByName("secp256k1");
        ecParams = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
        HALF_CURVE_ORDER = params.getN().shiftRight(1);
    }

    /** Strong random number generator */
    private static final SecureRandom secureRandom = new SecureRandom();

    /** Signed message header */
    private static final String BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n";

    /** Key label */
    private String label = "";

    /** Public key */
    private byte[] pubKey;

    /** Public key hash */
    private byte[] pubKeyHash;

    /** P2SH-P2WPKH script hash */
    private byte[] scriptHash;

    /** Private key */
    private BigInteger privKey;

    /** Key creation time (seconds) */
    private long creationTime;

    /** Compressed public key */
    private boolean isCompressed;

    /** Change key */
    private boolean isChange;

    /**
     * Creates an ECKey with a new public/private key pair.  Point compression is used
     * so the resulting public key will be 33 bytes (32 bytes for the x-coordinate and
     * 1 byte to represent the y-coordinate sign)
     */
    public ECKey() {
        ECKeyPairGenerator keyGenerator = new ECKeyPairGenerator();
        ECKeyGenerationParameters keyGenParams = new ECKeyGenerationParameters(ecParams, secureRandom);
        keyGenerator.init(keyGenParams);
        AsymmetricCipherKeyPair keyPair = keyGenerator.generateKeyPair();
        ECPrivateKeyParameters privKeyParams = (ECPrivateKeyParameters)keyPair.getPrivate();
        ECPublicKeyParameters pubKeyParams = (ECPublicKeyParameters)keyPair.getPublic();
        privKey = privKeyParams.getD();
        pubKey = pubKeyParams.getQ().getEncoded(true);
        creationTime = System.currentTimeMillis()/1000;
        isCompressed = true;
    }

    /**
     * Creates an ECKey with just a public key
     *
     * @param       pubKey              Public key
     */
    public ECKey(byte[] pubKey) {
        this(pubKey, null, false);
    }

    /**
     * Creates an ECKey public/private key pair using the supplied private key.  The
     * 'compressed' parameter determines the type of public key created.
     *
     * @param       privKey             Private key
     * @param       compressed          TRUE to create a compressed public key
     */
    public ECKey(BigInteger privKey, boolean compressed) {
        this(null, privKey, compressed);
    }

    /**
     * Creates an ECKey with the supplied public/private key pair.  The private key may be
     * null if you only want to use this ECKey to verify signatures.  The public key will
     * be generated from the private key if it is not provided (the 'compressed' parameter
     * determines the type of public key created)
     *
     * @param       pubKey              Public key or null
     * @param       privKey             Private key or null
     * @param       compressed          TRUE to create a compressed public key
     */
    public ECKey(byte[] pubKey, BigInteger privKey, boolean compressed) {
        this.privKey = privKey;
        if (pubKey != null) {
            this.pubKey = Arrays.copyOfRange(pubKey, 0, pubKey.length);
            isCompressed = (pubKey.length==33);
        } else if (privKey != null) {
            this.pubKey = pubKeyFromPrivKey(privKey, compressed);
            isCompressed = compressed;
        } else {
            throw new IllegalArgumentException("You must provide at least a private key or a public key");
        }
        creationTime = System.currentTimeMillis()/1000;
    }

    /**
     * Checks if the public key is canonical
     *
     * @param       pubKeyBytes         Public key
     * @return                          TRUE if the key is canonical
     */
    public static boolean isPubKeyCanonical(byte[] pubKeyBytes) {
        boolean isValid = false;
        if (pubKeyBytes.length == 33 && (pubKeyBytes[0] == (byte)0x02 || pubKeyBytes[0] == (byte)0x03)) {
            isValid = true;
        } else if (pubKeyBytes.length == 65 && pubKeyBytes[0] == (byte)0x04) {
            isValid = true;
        }
        return isValid;
    }

    /**
     * Checks if the signature is DER-encoded
     *
     * @param       encodedSig          Encoded signature
     * @return                          TRUE if the signature is DER-encoded
     */
    public static boolean isSignatureCanonical(byte[] encodedSig) {
        //
        // DER-encoding requires that there is only one representation for a given
        // encoding.  This means that no pad bytes are inserted for numeric values.
        //
        // An ASN.1 sequence is identified by 0x30 and each primitive by a type field.
        // An integer is identified as 0x02.  Each field type is followed by a field length.
        // For valid R and S values, the length is a single byte since R and S are both
        // 32-byte or 33-byte values (a leading zero byte is added to ensure a positive
        // value if the sign bit would otherwise bet set).
        //
        // Bitcoin appends that hash type to the end of the DER-encoded signature.  We require
        // this to be a single byte for a canonical signature.
        //
        // The length is encoded in the lower 7 bits for lengths between 0 and 127 and the upper bit is 0.
        // Longer length have the upper bit set to 1 and the lower 7 bits contain the number of bytes
        // in the length.
        //

        //
        // An ASN.1 sequence is 0x30 followed by the length
        //
        if (encodedSig.length<2 || encodedSig[0]!=(byte)0x30 || (encodedSig[1]&0x80)!=0)
            return false;
        //
        // Get length of sequence
        //
        int length = ((int)encodedSig[1]&0x7f) + 2;
        int offset = 2;
        //
        // Check R
        //
        if (offset+2>length || encodedSig[offset]!=(byte)0x02 || (encodedSig[offset+1]&0x80)!=0)
            return false;
        int rLength = (int)encodedSig[offset+1]&0x7f;
        if (offset+rLength+2 > length)
            return false;
        if (encodedSig[offset+2]==0x00 && (encodedSig[offset+3]&0x80)==0)
            return false;
        offset += rLength + 2;
        //
        // Check S
        //
        if (offset+2>length || encodedSig[offset]!=(byte)0x02 || (encodedSig[offset+1]&0x80)!=0)
            return false;
        int sLength = (int)encodedSig[offset+1]&0x7f;
        if (offset+sLength+2 > length)
            return false;
        if (encodedSig[offset+2]==0x00 && (encodedSig[offset+3]&0x80)==0)
            return false;
        offset += sLength + 2;
        //
        // There must be a single byte appended to the signature
        //
        return (offset == encodedSig.length-1);
    }

    /**
     * Returns the key creation time
     *
     * @return      Key creation time (seconds)
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Sets the key creation time
     *
     * @param       creationTime        Key creation time (seconds)
     */
    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    /**
     * Returns the key label
     *
     * @return      Key label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the key label
     *
     * @param       label               Key label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Checks if this is a change key
     *
     * @return                          TRUE if this is a change key
     */
    public boolean isChange() {
        return isChange;
    }

    /**
     * Sets change key status
     *
     * @param       isChange            TRUE if this is a change key
     */
    public void setChange(boolean isChange) {
        this.isChange = isChange;
    }

    /**
     * Returns the public key (as used in transaction scriptSigs).  A compressed
     * public key is 33 bytes and starts with '02' or '03' while an uncompressed
     * public key is 65 bytes and starts with '04'.
     *
     * @return                          Public key
     */
    public byte[] getPubKey() {
        return pubKey;
    }

    /**
     * Returns the public key hash as used in addresses.  The hash is 20 bytes.
     *
     * @return                          Public key hash
     */
    public byte[] getPubKeyHash() {
        if (pubKeyHash == null)
            pubKeyHash = Utils.sha256Hash160(pubKey);
        return pubKeyHash;
    }

    /**
     * Returns the address for this public key
     *
     * @return                          Address
     */
    public Address toAddress() {
        return new Address(getPubKeyHash(), label);
    }

    /**
     * Returns the private key
     *
     * @return                          Private key or null if there is no private key
     */
    public BigInteger getPrivKey() {
        return privKey;
    }

    /**
     * Return the private key bytes
     *
     * @return                          Private key bytes or null if there is no private key
     */
    public byte[] getPrivKeyBytes() {
        return (privKey!=null ? privKey.toByteArray() : null);
    }

    /**
     * Checks if there is a private key
     *
     * @return                          TRUE if there is a private key
     */
    public boolean hasPrivKey() {
        return (privKey!=null);
    }

    /**
     * Checks if the public key is compressed
     *
     * @return                          TRUE if the public key is compressed
     */
    public boolean isCompressed() {
        return isCompressed;
    }

    /**
     * Decompress a compressed public key (x coordinate and low-bit of y-coordinate).
     *
     * @param       xBN                 X-coordinate
     * @param       yBit                Sign of Y-coordinate
     * @return                          Uncompressed public key
     */
    private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
        SecP256K1Curve curve = (SecP256K1Curve)ecParams.getCurve();
        ECFieldElement x = curve.fromBigInteger(xBN);
        ECFieldElement alpha = x.multiply(x.square().add(curve.getA())).add(curve.getB());
        ECFieldElement beta = alpha.sqrt();
        if (beta == null)
            throw new IllegalArgumentException("Invalid point compression");
        ECPoint ecPoint;
        BigInteger nBeta = beta.toBigInteger();
        if (nBeta.testBit(0) == yBit) {
            ecPoint = curve.createPoint(x.toBigInteger(), nBeta);
        } else {
            ECFieldElement y = curve.fromBigInteger(curve.getQ().subtract(nBeta));
            ecPoint = curve.createPoint(x.toBigInteger(), y.toBigInteger());
        }
        return ecPoint;
    }

    /**
     * Get the public key ECPoint from the private key
     *
     * @param       privKey             Private key
     * @return                          Public key ECPoint
     */
    static ECPoint pubKeyPointFromPrivKey(BigInteger privKey) {
        BigInteger adjKey;
        if (privKey.bitLength() > ecParams.getN().bitLength()) {
            adjKey = privKey.mod(ecParams.getN());
        } else {
            adjKey = privKey;
        }
        return ecParams.getG().multiply(adjKey);
    }

    /**
     * Create the public key from the private key
     *
     * @param       privKey             Private key
     * @param       compressed          TRUE to generate a compressed public key
     * @return                          Public key
     */
    public static byte[] pubKeyFromPrivKey(BigInteger privKey, boolean compressed) {
        return pubKeyPointFromPrivKey(privKey).getEncoded(compressed);
    }

    /**
     * Checks if two objects are equal
     *
     * @param       obj             The object to check
     * @return                      TRUE if the object is equal
     */
    @Override
    public boolean equals(Object obj) {
        return (obj!=null && (obj instanceof ECKey) && Arrays.equals(pubKey, ((ECKey)obj).pubKey));
    }

    /**
     * Returns the hash code for this object
     *
     * @return                      Hash code
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(pubKey);
    }
}
