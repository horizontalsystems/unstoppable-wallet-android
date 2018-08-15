/**
 * Copyright 2013-2014 Ronald W Hoffman
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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * A script is a small program contained in the transaction which determines whether or not
 * an output can be spent.  The first half of the script is provided by the transaction input
 * and the second half of the script is provided by the transaction output.
 */
public class Script {

    /**
     * Checks that the script consists of only canonical push-data operations.
     *
     * For canonical scripts, each push-data operation must use the shortest opcode possible.
     * Numeric values between 0 and 16 must use OP_n opcodes.
     *
     * @param       scriptBytes     Script bytes
     * @return                      TRUE if only canonical push-data operations were found
     * @throws      EOFException    Script is too short
     */
    public static boolean checkInputScript(byte[] scriptBytes) throws EOFException {
        boolean scriptValid = true;
        int offset = 0;
        int length = scriptBytes.length;
        while (scriptValid && offset < length) {
            int opcode = ((int)scriptBytes[offset++])&0xff;
            if (opcode <= ScriptOpCodes.OP_PUSHDATA4) {
                int[] result = getDataLength(opcode, scriptBytes, offset);
                int dataLength = result[0];
                offset = result[1];
                if (dataLength == 1) {
                    if (opcode != 1 || ((int)scriptBytes[offset]&0xff) <= 16)
                        scriptValid = false;
                } else if (dataLength < 76) {
                    if (opcode >= ScriptOpCodes.OP_PUSHDATA1)
                        scriptValid = false;
                } else if (dataLength < 256) {
                    if (opcode != ScriptOpCodes.OP_PUSHDATA1)
                        scriptValid = false;
                } else if (dataLength < 65536) {
                    if (opcode != ScriptOpCodes.OP_PUSHDATA2)
                        scriptValid = false;
                }
                offset += dataLength;
                if (offset > length)
                    throw new EOFException("End-of-data while processing script");
            } else if (opcode > ScriptOpCodes.OP_16) {
                scriptValid = false;
            }
        }
        return scriptValid;
    }

    /**
     * Get the input data elements
     *
     * @param       scriptBytes     Script bytes
     * @return                      Data element list
     * @throws      EOFException    Script is too short
     */
    public static List<byte[]> getData(byte[] scriptBytes) throws EOFException {
        List<byte[]> dataList = new ArrayList<>();
        int offset = 0;
        int length = scriptBytes.length;
        while (offset<length) {
            int dataLength;
            int opcode = ((int)scriptBytes[offset++])&0xff;
            if (opcode <= ScriptOpCodes.OP_PUSHDATA4) {
                int[] result = getDataLength(opcode, scriptBytes, offset);
                dataLength = result[0];
                offset = result[1];
                if (dataLength > 0) {
                    if (offset+dataLength > length)
                        throw new EOFException("End-of-data while processing script");
                    dataList.add(Arrays.copyOfRange(scriptBytes, offset, offset+dataLength));
                    offset += dataLength;
                }
            }
        }
        return dataList;
    }

    /**
     * Checks script data elements against a Bloom filter
     *
     * @param       filter              Bloom filter
     * @param       scriptBytes         Script to check
     * @return                          TRUE if a data element in the script matched the filter
     */
//    public static boolean checkFilter(BloomFilter filter, byte[] scriptBytes) {
//        boolean foundMatch = false;
//        int offset = 0;
//        int length = scriptBytes.length;
//        //
//        // Check each data element in the script
//        //
//        try {
//            while (offset<length && !foundMatch) {
//                int dataLength;
//                int opcode = ((int)scriptBytes[offset++])&0xff;
//                if (opcode <= ScriptOpCodes.OP_PUSHDATA4) {
//                    //
//                    // Get the data element
//                    //
//                    int[] result = getDataLength(opcode, scriptBytes, offset);
//                    dataLength = result[0];
//                    offset = result[1];
//                    if (dataLength > 0) {
//                        if (offset+dataLength > length)
//                            throw new EOFException("End-of-data while processing script");
//                        foundMatch = filter.contains(scriptBytes, offset, dataLength);
//                        offset += dataLength;
//                    }
//                }
//            }
//        } catch (EOFException exc) {
//            // Invalid script - stop checking
//        }
//        return foundMatch;
//    }

    /**
     * Returns the payment type for an output script
     *
     * @param       scriptBytes         Script to check
     * @return      Payment type or 0 if not a standard payment type
     */
    public static int getPaymentType(byte[] scriptBytes) {
        int paymentType = 0;
        if (scriptBytes.length > 0) {
            if (scriptBytes[0] == (byte)ScriptOpCodes.OP_RETURN) {
                //
                // Scripts starting with OP_RETURN are unspendable
                //
                paymentType = ScriptOpCodes.PAY_TO_NOBODY;
            } else if (scriptBytes[0] == (byte)ScriptOpCodes.OP_DUP) {
                //
                // Check PAY_TO_PUBKEY_HASH
                //   OP_DUP OP_HASH160 <20-byte hash> OP_EQUALVERIFY OP_CHECKSIG
                //
                if (scriptBytes.length == 25 && scriptBytes[1] == (byte)ScriptOpCodes.OP_HASH160 &&
                                                scriptBytes[2] == 20 &&
                                                scriptBytes[23] == (byte)ScriptOpCodes.OP_EQUALVERIFY &&
                                                scriptBytes[24] == (byte)ScriptOpCodes.OP_CHECKSIG)
                    paymentType = ScriptOpCodes.PAY_TO_PUBKEY_HASH;
            } else if (((int)scriptBytes[0]&0xff) <= 65) {
                //
                // Check PAY_TO_PUBKEY
                //   <pubkey> OP_CHECKSIG
                //
                int length = (int)scriptBytes[0];
                if (scriptBytes.length == length+2 && scriptBytes[length+1] == (byte)ScriptOpCodes.OP_CHECKSIG)
                    paymentType = ScriptOpCodes.PAY_TO_PUBKEY;
            } else if (scriptBytes[0] == (byte)ScriptOpCodes.OP_HASH160) {
                //
                // Check PAY_TO_SCRIPT_HASH
                //   OP_HASH160 <20-byte hash> OP_EQUAL
                //
                if (scriptBytes.length == 23 && scriptBytes[1] == 20 &&
                                                scriptBytes[22] == (byte)ScriptOpCodes.OP_EQUAL)
                    paymentType = ScriptOpCodes.PAY_TO_SCRIPT_HASH;
            } else if (((int)scriptBytes[0]&0xff) >= 81 && ((int)scriptBytes[0]&0xff) <= 96) {
                //
                // Check PAY_TO_MULTISIG
                //   <m> <pubkey> <pubkey> ... <n> OP_CHECKMULTISIG
                //
                int offset = 1;
                while (offset < scriptBytes.length) {
                    int opcode = (int)scriptBytes[offset]&0xff;
                    if (opcode <= 65) {
                        //
                        // We have another pubkey - step over it
                        //
                        offset += opcode+1;
                        continue;
                    }
                    if (opcode >= 81 && opcode <= 96) {
                        //
                        // We have found <n>
                        //
                        if (scriptBytes.length == offset+2 &&
                                        scriptBytes[offset+1] == (byte)ScriptOpCodes.OP_CHECKMULTISIG)
                            paymentType = ScriptOpCodes.PAY_TO_MULTISIG;
                    }
                    break;
                }
            }
        }
        return paymentType;
    }

    /**
     * Build the ScriptPubKey
     *
     * @param       address             Payment address
     * @param       encodeLength        TRUE to encode the script length
     * @return                          ScriptPubKey
     */
    public static byte[] getScriptPubKey(Address address, boolean encodeLength) {
        return getScriptPubKey(address.getType(), address.getHash(), encodeLength);
    }

    /**
     * Build the ScriptPubKey
     *
     * @param       type                Address type
     * @param       hash                Address hash
     * @param       encodeLength        TRUE to encode the script length
     * @return                          ScriptPubKey
     */
    public static byte[] getScriptPubKey(Address.AddressType type, byte[] hash, boolean encodeLength) {
        byte[] scriptBytes;
        int base;
        switch (type) {
            case P2PKH:
                if (encodeLength) {
                    scriptBytes = new byte[26];
                    scriptBytes[0] = (byte)25;
                    base = 1;
                } else {
                    scriptBytes = new byte[25];
                    base = 0;
                }
                scriptBytes[base+0] = (byte)ScriptOpCodes.OP_DUP;
                scriptBytes[base+1] = (byte)ScriptOpCodes.OP_HASH160;
                scriptBytes[base+2] = (byte)20;
                System.arraycopy(hash, 0, scriptBytes, base+3, 20);
                scriptBytes[base+23] = (byte)ScriptOpCodes.OP_EQUALVERIFY;
                scriptBytes[base+24] = (byte)ScriptOpCodes.OP_CHECKSIG;
                break;
            case P2SH:
                if (encodeLength) {
                    scriptBytes = new byte[24];
                    scriptBytes[0] = (byte)23;
                    base = 1;
                } else {
                    scriptBytes = new byte[23];
                    base = 0;
                }
                scriptBytes[base+0] = (byte)ScriptOpCodes.OP_HASH160;
                scriptBytes[base+1] = (byte)20;
                System.arraycopy(hash, 0, scriptBytes, base+2, 20);
                scriptBytes[base+22] = (byte)ScriptOpCodes.OP_EQUAL;
                break;
            default:
                throw new IllegalArgumentException("Unsupported address type");
        }
        return scriptBytes;
    }

    /**
     * Build the P2SH-P2WPKH redeem script
     *
     * OP_0 <pubkey-hash>
     *
     * @param       pubKeyHash          Public key hash
     * @param       encodeLength        TRUE to encode the script length
     * @return                          P2SH redeem script
     */
    public static byte[] getRedeemScript(byte[] pubKeyHash, boolean encodeLength) {
        if (pubKeyHash.length != 20)
            throw new IllegalArgumentException("Public key hash is not 20 bytes");
        byte[] scriptBytes;
        int base;
        if (encodeLength) {
            scriptBytes = new byte[23];
            scriptBytes[0] =(byte)22;
            base = 1;
        } else {
            scriptBytes = new byte[22];
            base = 0;
        }
        scriptBytes[base+0] = (byte)ScriptOpCodes.OP_0;
        scriptBytes[base+1] = (byte)20;
        System.arraycopy(pubKeyHash, 0, scriptBytes, base+2, 20);
        return scriptBytes;
    }

    /**
     * Build the P2WPKH witness program
     *
     * OP_DUP OP_HASH160 <pubkey-hash> OP_EQUALVERIFY OP_CHECKSIG
     *
     * @param       pubKeyHash          Public key hash
     * @param       encodeLength        TRUE to encode the script length
     * @return                          P2WPKH witness program
     */
    public static byte[] getWitnessProgram(byte[] pubKeyHash, boolean encodeLength) {
        if (pubKeyHash.length != 20)
            throw new IllegalArgumentException("Public key hash is not 20 bytes");
        byte[] scriptBytes;
        int base;
        if (encodeLength) {
            scriptBytes = new byte[26];
            scriptBytes[0] = (byte)25;
            base = 1;
        } else {
            scriptBytes = new byte[25];
            base = 0;
        }
        scriptBytes[base+0] = (byte)ScriptOpCodes.OP_DUP;
        scriptBytes[base+1] = (byte)ScriptOpCodes.OP_HASH160;
        scriptBytes[base+2] = (byte)20;
        System.arraycopy(pubKeyHash, 0, scriptBytes, base+3, 20);
        scriptBytes[base+23] = (byte)ScriptOpCodes.OP_EQUALVERIFY;
        scriptBytes[base+24] = (byte)ScriptOpCodes.OP_CHECKSIG;
        return scriptBytes;
    }

    /**
     * Get the witness program
     *
     * A witness script consists of exactly two data push operations.  The first
     * push defines the version and the second push is the witness program.
     *
     * The return value consists of two values: the first value is the version
     * and the second value is the witness program.
     *
     * @param       scriptBytes         Script bytes
     * @return                          Witness program or null if not a witness script
     * @throws      ScriptException     Script is not valid
     */
//    public static byte[][] getWitnessProgram(byte[] scriptBytes) throws ScriptException {
//        byte[][] witnessBytes = null;
//        if (scriptBytes.length >= 2 &&
//                scriptBytes[0] >= (byte)0 && scriptBytes[0] <= (byte)16 &&
//                scriptBytes[1] >= (byte)0 && scriptBytes[1] <= (byte)40) {
//            int version = (int)scriptBytes[0]&0xff;
//            int length = (int)scriptBytes[1]&0xff;
//            if (length+2 > scriptBytes.length)
//                throw new ScriptException("End of script reached processing witness program");
//            if (length+2 == scriptBytes.length) {
//                if (version == 0 && length != 20 && length != 32)
//                        throw new ScriptException("Version 0 witness program length is not 20 or 32");
//                witnessBytes = new byte[2][];
//                witnessBytes[0] = new byte[1];
//                witnessBytes[0][0] = (byte)version;
//                witnessBytes[1] = new byte[length];
//                System.arraycopy(scriptBytes, 2, witnessBytes[1], 0, length);
//            }
//        }
//        return witnessBytes;
//    }

    /**
     * Get the length of the next data element
     *
     * @param       opcode              Current opcode
     * @param       scriptBytes         Script program
     * @param       startOffset         Offset to byte following the opcode
     * @return      Array containing the data length and the offset to the data
     * @throws      EOFException        Script is too short
     */
    public static int[] getDataLength(int opcode, byte[] scriptBytes, int startOffset) throws EOFException {
        int[] result = new int[2];
        int offset = startOffset;
        int dataToRead;
        if (opcode < ScriptOpCodes.OP_PUSHDATA1) {
            // These opcodes push data with a length equal to the opcode
            dataToRead = opcode;
        } else if (opcode == ScriptOpCodes.OP_PUSHDATA1) {
            // The data length is in the next byte
            if (offset > scriptBytes.length-1)
                throw new EOFException("End-of-data while processing script");
            dataToRead = (int)scriptBytes[offset]&0xff;
            offset++;
        } else if (opcode == ScriptOpCodes.OP_PUSHDATA2) {
            // The data length is in the next two bytes
            if (offset > scriptBytes.length-2)
                throw new EOFException("End-of-data while processing script");
            dataToRead = ((int)scriptBytes[offset]&0xff) | (((int)scriptBytes[offset+1]&0xff)<<8);
            offset += 2;
        } else if (opcode == ScriptOpCodes.OP_PUSHDATA4) {
            // The data length is in the next four bytes
            if (offset > scriptBytes.length-4)
                throw new EOFException("End-of-data while processing script");
            dataToRead = ((int)scriptBytes[offset]&0xff) |
                                    (((int)scriptBytes[offset+1]&0xff)<<8) |
                                    (((int)scriptBytes[offset+2]&0xff)<<16) |
                                    (((int)scriptBytes[offset+3]&0xff)<<24);
            offset += 4;
        } else {
            dataToRead = 0;
        }
        result[0] = dataToRead;
        result[1] = offset;
        return result;
    }

    /**
     * Remove all instances of a data element from a script
     *
     * @param       dataBytes           The bytes to be removed
     * @param       scriptBytes         The script bytes
     * @param       lastSeparator       Last code separator
     * @return                          Script subprogram with data element removed
     */
    public static byte[] removeDataElement(byte[] dataBytes, byte[] scriptBytes, int lastSeparator) {
        byte[] subProgram;
        try {
            try (ByteArrayOutputStream outStream = new ByteArrayOutputStream(scriptBytes.length)) {
                int index = lastSeparator;
                int count = scriptBytes.length;
                while (index < count) {
                    int startPos = index;
                    int dataLength = 0;
                    int opcode = ((int)scriptBytes[index++])&0x00ff;
                    if (opcode <= ScriptOpCodes.OP_PUSHDATA4) {
                        int result[] = Script.getDataLength(opcode, scriptBytes, index);
                        dataLength = result[0];
                        index = result[1];
                    }
                    boolean copyElement = true;
                    if (dataLength == dataBytes.length) {
                        copyElement = false;
                        for (int i=0; i<dataLength; i++) {
                            if (dataBytes[i] != scriptBytes[index+i]) {
                                copyElement = true;
                                break;
                            }
                        }
                    }
                    if (copyElement)
                        outStream.write(scriptBytes, startPos, index-startPos+dataLength);
                    index += dataLength;
                }
                subProgram = outStream.toByteArray();
            }
        } catch (IOException exc) {
            throw new RuntimeException("Unexpected I/O error from ByteArrayOutputStream", exc);
        }
        return subProgram;
    }
}
