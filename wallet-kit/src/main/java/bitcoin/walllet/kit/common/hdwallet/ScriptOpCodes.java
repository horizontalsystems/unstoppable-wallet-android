/*
 * Copyright 2013 Google Inc.
 * Copyright 2013-2017 Ronald W Hoffman
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

import java.util.HashMap;
import java.util.Map;

/**
 * Various constants that define the assembly-like scripting language that forms part of the Bitcoin protocol.
 */
public class ScriptOpCodes {

    /** Standard signature types */
    public static final int PAY_TO_PUBKEY_HASH = 1;
    public static final int PAY_TO_PUBKEY = 2;
    public static final int PAY_TO_SCRIPT_HASH = 3;
    public static final int PAY_TO_MULTISIG = 4;
    public static final int PAY_TO_NOBODY = 5;

    /** Signature hash types */
    public static final int SIGHASH_ALL = 1;
    public static final int SIGHASH_NONE = 2;
    public static final int SIGHASH_SINGLE = 3;
    public static final int SIGHASH_FORKID = 64;
    public static final int SIGHASH_ANYONE_CAN_PAY = 128;

    // Push value
    public static final int OP_0 = 0x00;
    public static final int OP_FALSE = OP_0;
    public static final int OP_PUSHDATA1 = 0x4c;
    public static final int OP_PUSHDATA2 = 0x4d;
    public static final int OP_PUSHDATA4 = 0x4e;
    public static final int OP_1NEGATE = 0x4f;
    public static final int OP_RESERVED = 0x50;
    public static final int OP_1 = 0x51;
    public static final int OP_TRUE = OP_1;
    public static final int OP_2 = 0x52;
    public static final int OP_3 = 0x53;
    public static final int OP_4 = 0x54;
    public static final int OP_5 = 0x55;
    public static final int OP_6 = 0x56;
    public static final int OP_7 = 0x57;
    public static final int OP_8 = 0x58;
    public static final int OP_9 = 0x59;
    public static final int OP_10 = 0x5a;
    public static final int OP_11 = 0x5b;
    public static final int OP_12 = 0x5c;
    public static final int OP_13 = 0x5d;
    public static final int OP_14 = 0x5e;
    public static final int OP_15 = 0x5f;
    public static final int OP_16 = 0x60;
    // Control
    public static final int OP_NOP = 0x61;
    public static final int OP_VER = 0x62;
    public static final int OP_IF = 0x63;
    public static final int OP_NOTIF = 0x64;
    public static final int OP_VERIF = 0x65;
    public static final int OP_VERNOTIF = 0x66;
    public static final int OP_ELSE = 0x67;
    public static final int OP_ENDIF = 0x68;
    public static final int OP_VERIFY = 0x69;
    public static final int OP_RETURN = 0x6a;
    // Stack ops
    public static final int OP_TOALTSTACK = 0x6b;
    public static final int OP_FROMALTSTACK = 0x6c;
    public static final int OP_2DROP = 0x6d;
    public static final int OP_2DUP = 0x6e;
    public static final int OP_3DUP = 0x6f;
    public static final int OP_2OVER = 0x70;
    public static final int OP_2ROT = 0x71;
    public static final int OP_2SWAP = 0x72;
    public static final int OP_IFDUP = 0x73;
    public static final int OP_DEPTH = 0x74;
    public static final int OP_DROP = 0x75;
    public static final int OP_DUP = 0x76;
    public static final int OP_NIP = 0x77;
    public static final int OP_OVER = 0x78;
    public static final int OP_PICK = 0x79;
    public static final int OP_ROLL = 0x7a;
    public static final int OP_ROT = 0x7b;
    public static final int OP_SWAP = 0x7c;
    public static final int OP_TUCK = 0x7d;
    // Splice ops
    public static final int OP_CAT = 0x7e;
    public static final int OP_SUBSTR = 0x7f;
    public static final int OP_LEFT = 0x80;
    public static final int OP_RIGHT = 0x81;
    public static final int OP_SIZE = 0x82;
    // Bit logic
    public static final int OP_INVERT = 0x83;
    public static final int OP_AND = 0x84;
    public static final int OP_OR = 0x85;
    public static final int OP_XOR = 0x86;
    public static final int OP_EQUAL = 0x87;
    public static final int OP_EQUALVERIFY = 0x88;
    public static final int OP_RESERVED1 = 0x89;
    public static final int OP_RESERVED2 = 0x8a;
    // Numeric
    public static final int OP_1ADD = 0x8b;
    public static final int OP_1SUB = 0x8c;
    public static final int OP_2MUL = 0x8d;
    public static final int OP_2DIV = 0x8e;
    public static final int OP_NEGATE = 0x8f;
    public static final int OP_ABS = 0x90;
    public static final int OP_NOT = 0x91;
    public static final int OP_0NOTEQUAL = 0x92;
    public static final int OP_ADD = 0x93;
    public static final int OP_SUB = 0x94;
    public static final int OP_MUL = 0x95;
    public static final int OP_DIV = 0x96;
    public static final int OP_MOD = 0x97;
    public static final int OP_LSHIFT = 0x98;
    public static final int OP_RSHIFT = 0x99;
    public static final int OP_BOOLAND = 0x9a;
    public static final int OP_BOOLOR = 0x9b;
    public static final int OP_NUMEQUAL = 0x9c;
    public static final int OP_NUMEQUALVERIFY = 0x9d;
    public static final int OP_NUMNOTEQUAL = 0x9e;
    public static final int OP_LESSTHAN = 0x9f;
    public static final int OP_GREATERTHAN = 0xa0;
    public static final int OP_LESSTHANOREQUAL = 0xa1;
    public static final int OP_GREATERTHANOREQUAL = 0xa2;
    public static final int OP_MIN = 0xa3;
    public static final int OP_MAX = 0xa4;
    public static final int OP_WITHIN = 0xa5;
    // Crypto
    public static final int OP_RIPEMD160 = 0xa6;
    public static final int OP_SHA1 = 0xa7;
    public static final int OP_SHA256 = 0xa8;
    public static final int OP_HASH160 = 0xa9;
    public static final int OP_HASH256 = 0xaa;
    public static final int OP_CODESEPARATOR = 0xab;
    public static final int OP_CHECKSIG = 0xac;
    public static final int OP_CHECKSIGVERIFY = 0xad;
    public static final int OP_CHECKMULTISIG = 0xae;
    public static final int OP_CHECKMULTISIGVERIFY = 0xaf;
    // Expansion
    public static final int OP_NOP1 = 0xb0;
    public static final int OP_CHECKLOCKTIMEVERIFY = 0xb1;
    public static final int OP_CHECKSEQUENCEVERIFY = 0xb2;
    public static final int OP_NOP4 = 0xb3;
    public static final int OP_NOP5 = 0xb4;
    public static final int OP_NOP6 = 0xb5;
    public static final int OP_NOP7 = 0xb6;
    public static final int OP_NOP8 = 0xb7;
    public static final int OP_NOP9 = 0xb8;
    public static final int OP_NOP10 = 0xb9;
    public static final int OP_INVALIDOPCODE = 0xff;

    private static final Map<Integer, String> opCodeMap = new HashMap<>(125);
    static {
        opCodeMap.put(OP_0, "0");
        opCodeMap.put(OP_PUSHDATA1, "PUSHDATA1");
        opCodeMap.put(OP_PUSHDATA2, "PUSHDATA2");
        opCodeMap.put(OP_PUSHDATA4, "PUSHDATA4");
        opCodeMap.put(OP_1NEGATE, "1NEGATE");
        opCodeMap.put(OP_RESERVED, "RESERVED");
        opCodeMap.put(OP_1, "1");
        opCodeMap.put(OP_2, "2");
        opCodeMap.put(OP_3, "3");
        opCodeMap.put(OP_4, "4");
        opCodeMap.put(OP_5, "5");
        opCodeMap.put(OP_6, "6");
        opCodeMap.put(OP_7, "7");
        opCodeMap.put(OP_8, "8");
        opCodeMap.put(OP_9, "9");
        opCodeMap.put(OP_10, "10");
        opCodeMap.put(OP_11, "11");
        opCodeMap.put(OP_12, "12");
        opCodeMap.put(OP_13, "13");
        opCodeMap.put(OP_14, "14");
        opCodeMap.put(OP_15, "15");
        opCodeMap.put(OP_16, "16");
        opCodeMap.put(OP_NOP, "NOP");
        opCodeMap.put(OP_VER, "VER");
        opCodeMap.put(OP_IF, "IF");
        opCodeMap.put(OP_NOTIF, "NOTIF");
        opCodeMap.put(OP_VERIF, "VERIF");
        opCodeMap.put(OP_VERNOTIF, "VERNOTIF");
        opCodeMap.put(OP_ELSE, "ELSE");
        opCodeMap.put(OP_ENDIF, "ENDIF");
        opCodeMap.put(OP_VERIFY, "VERIFY");
        opCodeMap.put(OP_RETURN, "RETURN");
        opCodeMap.put(OP_TOALTSTACK, "TOALTSTACK");
        opCodeMap.put(OP_FROMALTSTACK, "FROMALTSTACK");
        opCodeMap.put(OP_2DROP, "2DROP");
        opCodeMap.put(OP_2DUP, "2DUP");
        opCodeMap.put(OP_3DUP, "3DUP");
        opCodeMap.put(OP_2OVER, "2OVER");
        opCodeMap.put(OP_2ROT, "2ROT");
        opCodeMap.put(OP_2SWAP, "2SWAP");
        opCodeMap.put(OP_IFDUP, "IFDUP");
        opCodeMap.put(OP_DEPTH, "DEPTH");
        opCodeMap.put(OP_DROP, "DROP");
        opCodeMap.put(OP_DUP, "DUP");
        opCodeMap.put(OP_NIP, "NIP");
        opCodeMap.put(OP_OVER, "OVER");
        opCodeMap.put(OP_PICK, "PICK");
        opCodeMap.put(OP_ROLL, "ROLL");
        opCodeMap.put(OP_ROT, "ROT");
        opCodeMap.put(OP_SWAP, "SWAP");
        opCodeMap.put(OP_TUCK, "TUCK");
        opCodeMap.put(OP_CAT, "CAT");
        opCodeMap.put(OP_SUBSTR, "SUBSTR");
        opCodeMap.put(OP_LEFT, "LEFT");
        opCodeMap.put(OP_RIGHT, "RIGHT");
        opCodeMap.put(OP_SIZE, "SIZE");
        opCodeMap.put(OP_INVERT, "INVERT");
        opCodeMap.put(OP_AND, "AND");
        opCodeMap.put(OP_OR, "OR");
        opCodeMap.put(OP_XOR, "XOR");
        opCodeMap.put(OP_EQUAL, "EQUAL");
        opCodeMap.put(OP_EQUALVERIFY, "EQUALVERIFY");
        opCodeMap.put(OP_RESERVED1, "RESERVED1");
        opCodeMap.put(OP_RESERVED2, "RESERVED2");
        opCodeMap.put(OP_1ADD, "1ADD");
        opCodeMap.put(OP_1SUB, "1SUB");
        opCodeMap.put(OP_2MUL, "2MUL");
        opCodeMap.put(OP_2DIV, "2DIV");
        opCodeMap.put(OP_NEGATE, "NEGATE");
        opCodeMap.put(OP_ABS, "ABS");
        opCodeMap.put(OP_NOT, "NOT");
        opCodeMap.put(OP_0NOTEQUAL, "0NOTEQUAL");
        opCodeMap.put(OP_ADD, "ADD");
        opCodeMap.put(OP_SUB, "SUB");
        opCodeMap.put(OP_MUL, "MUL");
        opCodeMap.put(OP_DIV, "DIV");
        opCodeMap.put(OP_MOD, "MOD");
        opCodeMap.put(OP_LSHIFT, "LSHIFT");
        opCodeMap.put(OP_RSHIFT, "RSHIFT");
        opCodeMap.put(OP_BOOLAND, "BOOLAND");
        opCodeMap.put(OP_BOOLOR, "BOOLOR");
        opCodeMap.put(OP_NUMEQUAL, "NUMEQUAL");
        opCodeMap.put(OP_NUMEQUALVERIFY, "NUMEQUALVERIFY");
        opCodeMap.put(OP_NUMNOTEQUAL, "NUMNOTEQUAL");
        opCodeMap.put(OP_LESSTHAN, "LESSTHAN");
        opCodeMap.put(OP_GREATERTHAN, "GREATERTHAN");
        opCodeMap.put(OP_LESSTHANOREQUAL, "LESSTHANOREQUAL");
        opCodeMap.put(OP_GREATERTHANOREQUAL, "GREATERTHANOREQUAL");
        opCodeMap.put(OP_MIN, "MIN");
        opCodeMap.put(OP_MAX, "MAX");
        opCodeMap.put(OP_WITHIN, "WITHIN");
        opCodeMap.put(OP_RIPEMD160, "RIPEMD160");
        opCodeMap.put(OP_SHA1, "SHA1");
        opCodeMap.put(OP_SHA256, "SHA256");
        opCodeMap.put(OP_HASH160, "HASH160");
        opCodeMap.put(OP_HASH256, "HASH256");
        opCodeMap.put(OP_CODESEPARATOR, "CODESEPARATOR");
        opCodeMap.put(OP_CHECKSIG, "CHECKSIG");
        opCodeMap.put(OP_CHECKSIGVERIFY, "CHECKSIGVERIFY");
        opCodeMap.put(OP_CHECKMULTISIG, "CHECKMULTISIG");
        opCodeMap.put(OP_CHECKMULTISIGVERIFY, "CHECKMULTISIGVERIFY");
        opCodeMap.put(OP_NOP1, "NOP1");
        opCodeMap.put(OP_CHECKLOCKTIMEVERIFY, "CHECKLOCKTIMEVERIFY");
        opCodeMap.put(OP_CHECKSEQUENCEVERIFY, "CHECKSEQUENCEVERIFY");
        opCodeMap.put(OP_NOP4, "NOP4");
        opCodeMap.put(OP_NOP5, "NOP5");
        opCodeMap.put(OP_NOP6, "NOP6");
        opCodeMap.put(OP_NOP7, "NOP7");
        opCodeMap.put(OP_NOP8, "NOP8");
        opCodeMap.put(OP_NOP9, "NOP9");
        opCodeMap.put(OP_NOP10, "NOP10");
    }

    /**
     * Converts the given OpCode into a string (eg "0", "PUSHDATA", or "NON_OP(10)")
     *
     * @param       opcode          Opcode
     * @return                      String result
     */
    public static String getOpCodeName(int opcode) {
        if (opCodeMap.containsKey(opcode))
            return opCodeMap.get(opcode);
        return "NON_OP(" + opcode + ")";
    }
}
