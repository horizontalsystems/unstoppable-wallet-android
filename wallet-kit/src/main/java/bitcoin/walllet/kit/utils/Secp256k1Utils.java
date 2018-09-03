package bitcoin.walllet.kit.utils;

import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERSequenceGenerator;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.spongycastle.asn1.ASN1Integer;
import org.spongycastle.util.Arrays;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Provider;

public class Secp256k1Utils {

    public static final Provider BC = new BouncyCastleProvider();

    public static final ECParameterSpec SPEC = ECNamedCurveTable.getParameterSpec("secp256k1");

    public static final X9ECParameters PARAMS = SECNamedCurves.getByName("secp256k1");
    public static final ECDomainParameters ECPARAMS = new ECDomainParameters(PARAMS.getCurve(), PARAMS.getG(),
            PARAMS.getN(), PARAMS.getH());
    final static ECCurve curve = SPEC.getCurve();

    final static ECPoint G = SPEC.getG();
    final static BigInteger N = SPEC.getN();
    final static BigInteger H = SPEC.getH();

    public static byte[] sign(byte[] input, BigInteger privateKey) {
        ECDSASigner signer = new ECDSASigner();
        ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(privateKey, ECPARAMS);
        signer.init(true, privKey);
        BigInteger[] sigs = signer.generateSignature(input);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            DERSequenceGenerator seq = new DERSequenceGenerator(output);
            seq.addObject((DEREncodable) new ASN1Integer(sigs[0]));
            seq.addObject((DEREncodable) new ASN1Integer(sigs[1]));
            seq.close();
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

//	public static boolean verify(byte[] input, byte[] signature, byte[] publicKey) {
//		ECDSASigner signer = new ECDSASigner();
//		ECPublicKeyParameters params = new ECPublicKeyParameters(ECPARAMS.getCurve().decodePoint(publicKey), ECPARAMS);
//		signer.init(false, params);
//		try {
//			ASN1InputStream decoder = new ASN1InputStream(signature);
//			DLSequence seq = (DLSequence) decoder.readObject();
//			ASN1Integer r = (ASN1Integer) seq.getObjectAt(0);
//			ASN1Integer s = (ASN1Integer) seq.getObjectAt(1);
//			decoder.close();
//			return signer.verifySignature(input, r.getValue(), s.getValue());
//		} catch (IOException e) {
//			throw new RuntimeException(e);
//		}
//	}

    public static byte[] toPublicKey(BigInteger privateKey) {
//		return ECPARAMS.getG().multiply(privateKey).getEncoded(false);
        return ECPARAMS.getG().multiply(privateKey).getEncoded();
    }

    /**
     * Public key prefix: 0x04.
     */
//    private static final byte PUBLIC_KEY_PREFIX = 0x04;
//    private static final byte[] PUBLIC_KEY_PREFIX_ARRAY = {PUBLIC_KEY_PREFIX};
//
//    public static byte[] toPublicKey(BigInteger[] pubKeys) {
//        byte[] xs = bigIntegerToBytes(pubKeys[0], 32);
//        byte[] ys = bigIntegerToBytes(pubKeys[1], 32);
//        return BytesUtils.concat(PUBLIC_KEY_PREFIX_ARRAY, xs, ys);
//    }

    static byte[] bigIntegerToBytes(BigInteger bi, int length) {
        byte[] data = bi.toByteArray();
        if (data.length == length) {
            return data;
        }
        // remove leading zero:
        if (data[0] == 0) {
            data = Arrays.copyOfRange(data, 1, data.length);
        }
        if (data.length > length) {
            throw new IllegalArgumentException("BigInteger is too large.");
        }
        byte[] copy = new byte[length];
        System.arraycopy(data, 0, copy, length - data.length, data.length);
        return copy;
    }

    public static ECPoint getPoint(BigInteger k) {
        return G.multiply(k.mod(SPEC.getN()));
    }

    public static ECPoint getG() {
        return G;
    }

    public static BigInteger getN() {
        return N;
    }

    public static BigInteger getH() {
        return H;
    }

    public static int getFieldSize() {
        return curve.getFieldSize();
    }

    public static ECCurve getCurve() {
        return curve;
    }

//    public static String uncompressedPublicKeyToAddress(byte[] uncompressed) {
//        if (uncompressed.length != 65) {
//            throw new IllegalArgumentException(
//                    "bad length of uncompressed bytes: expect 65 but actual " + uncompressed.length);
//        }
//        byte[] hash160 = HashUtils.ripeMd160(HashUtils.sha256(uncompressed));
//        return _hash160PublicKeyToAddress(hash160);
//    }
//
//    public static String compressedPublicKeyToAddress(byte[] compressed) {
//        if (compressed.length != 33) {
//            throw new IllegalArgumentException(
//                    "bad length of compressed bytes: expect 33 but actual " + compressed.length);
//        }
//        byte[] hash160 = HashUtils.ripeMd160(HashUtils.sha256(compressed));
//        return _hash160PublicKeyToAddress(hash160);
//    }
//
//    public static String hash160PublicKeyToAddress(byte[] hash160) {
//        if (hash160.length != 20) {
//            throw new IllegalArgumentException("bad length of hash160 bytes: expect 20 but actual " + hash160.length);
//        }
//        return _hash160PublicKeyToAddress(hash160);
//    }

    /**
     * Network ID: 0x00 = main network.
     */
//    private static final byte NETWORK_ID = 0x00;
//    private static final byte[] NETWORK_ID_ARRAY = {NETWORK_ID};
//
//    private static String _hash160PublicKeyToAddress(byte[] hash160) {
//        byte[] hashWithNetworkId = BytesUtils.concat(NETWORK_ID_ARRAY, hash160);
//        byte[] checksum = HashUtils.doubleSha256(hashWithNetworkId);
//        byte[] address = BytesUtils.concat(hashWithNetworkId, Arrays.copyOfRange(checksum, 0, 4));
//        return Base58Utils.encode(address);
//    }
//
//    public static byte[] publicKeyAddressToBytes(String address) {
//        byte[] data = Base58Utils.decodeChecked(address);
//        if (data.length != 21) {
//            throw new IllegalArgumentException("bad length of decoded bytes: expect 21 but actual " + data.length);
//        }
//        if (data[0] != NETWORK_ID) {
//            throw new IllegalArgumentException("Leading byte is not 0x00.");
//        }
//        return Arrays.copyOfRange(data, 1, data.length);
//    }

}
