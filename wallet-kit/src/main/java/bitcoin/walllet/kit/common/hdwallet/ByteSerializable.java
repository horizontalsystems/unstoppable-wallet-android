/*
 * Copyright 2014 Ronald Hoffman.
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

/**
 * An object supporting the ByteSerializable interface provides the getBytes() method
 * to serialize the object.
 */
public interface ByteSerializable {

    /**
     * Return a serialized byte array
     *
     * @return                          Serialized byte array
     */
    public byte[] getBytes();

    /**
     * Write the object to a serialized buffer
     *
     * @param       buffer              Serialized buffer
     * @return                          Serialized buffer
     */
    public SerializedBuffer getBytes(SerializedBuffer buffer);
}
