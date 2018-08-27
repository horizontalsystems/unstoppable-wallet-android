/**
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
package bitcoin.walllet.kit.hdwallet;

import java.util.Arrays;

import bitcoin.walllet.kit.constant.BitcoinConstants;
import bitcoin.walllet.kit.exceptions.AddressFormatException;
import bitcoin.walllet.kit.utils.Base58Utils;

/**
 * An address is a 20-byte Hash160 of a public key or script.  The displayable form
 * is Base-58 encoded with a 1-byte version and a 4-byte checksum.
 */
public class Address {

    /** Address type */
    public enum AddressType {
        P2PKH,                      // Pay to public key hash
        P2SH                        // Pay to script hash
    }

    /** Address label */
    private String label;

    /** Address hash */
    private byte[] hash;

    /** Address type */
    private AddressType type;

    /**
     * Creates a new Address with a zero hash
     */
    public Address() {
        this (AddressType.P2PKH, new byte[20], "");
    }

    /**
     * Creates a new P2PKH Address using the 20-byte hash
     *
     * @param       hash                    Hash
     */
    public Address(byte[] hash) {
        this(AddressType.P2PKH, hash, "");
    }

    /**
     * Creates a new P2PKH Address using the 20-byte hash and a label
     *
     * @param       hash                    Hash
     * @param       label                   Address label
     */
    public Address(byte[] hash, String label) {
        this(AddressType.P2PKH, hash, label);
    }

    /**
     * Creates a new Address using the 20-byte hash
     *
     * @param       type                    Address type
     * @param       hash                    Hash
     */
    public Address(AddressType type, byte[] hash) {
        this(type, hash, "");
    }

    /**
     * Creates a new Address using the 20-byte hash and a label
     *
     * @param       type                    Address type
     * @param       hash                    Hash
     * @param       label                   Address label
     */
    public Address(AddressType type, byte[] hash, String label) {
        this.hash = hash;
        this.label = label;
        this.type = type;
    }

    /**
     * Creates a new Address using a Base-58 encoded address
     *
     * @param       address                 Encoded address
     * @throws AddressFormatException  Address string is not a valid address
     */
    public Address(String address) throws AddressFormatException {
        this(address, "");
    }

    /**
     * Creates a new Address using a Base-58 encoded address and a label
     *
     * @param       address                 Encoded address
     * @param       label                   Address label
     * @throws      AddressFormatException  Address string is not valid
     */
    public Address(String address, String label) throws AddressFormatException {
        //
        // Set the label
        //
        this.label = label;
        //
        // Decode the address
        //
        byte[] decoded = Base58Utils.decodeChecked(address);
        int version = (int)decoded[0]&0xff;
        if (version == BitcoinConstants.ADDRESS_VERSION) {
            type = AddressType.P2PKH;
        } else if (version == BitcoinConstants.SCRIPT_ADDRESS_VERSION) {
            type = AddressType.P2SH;
        } else {
            throw new AddressFormatException(String.format("Address version %d is not correct", version));
        }
        //
        // The address must be 20 bytes
        //
        if (decoded.length != 20+1)
            throw new AddressFormatException("Address length is not 20 bytes");
        //
        // Get the address hash
        //
        hash = Arrays.copyOfRange(decoded, 1, decoded.length);
    }

    /**
     * Returns the address label
     *
     * @return      Address label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the address label
     *
     * @param       label           Address label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the address hash
     *
     * @return      20-byte address hash
     */
    public byte[] getHash() {
        return hash;
    }

    /**
     * Return the address type
     *
     * @return      Address type
     */
    public AddressType getType() {
        return type;
    }

    /**
     * Returns the address as a Base58-encoded string with a 1-byte version
     * and a 4-byte checksum
     *
     * @return      Encoded string
     */
    @Override
    public String toString() {
        byte[] addressBytes = new byte[1+hash.length+4];
        if (type == AddressType.P2PKH) {
            addressBytes[0] = (byte)BitcoinConstants.ADDRESS_VERSION;
        } else {
            addressBytes[0] = (byte)BitcoinConstants.SCRIPT_ADDRESS_VERSION;
        }
        System.arraycopy(hash, 0, addressBytes, 1, hash.length);
        byte[] digest = Utils.doubleDigest(addressBytes, 0, hash.length+1);
        System.arraycopy(digest, 0, addressBytes, hash.length+1, 4);
        return Base58Utils.encode(addressBytes);
    }

    /**
     * Returns the hash code for this object
     *
     * @return      Hash code
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(hash);
    }

    /**
     * Checks if two objects are equal
     *
     * @param       obj             The object to check
     * @return                      TRUE if the objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        return (obj!=null && (obj instanceof Address) &&
                type==((Address)obj).type && Arrays.equals(hash, ((Address)obj).hash));
    }
}
