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

/**
 * A SignatureException is thrown if a message signature is not valid
 */
public class SignatureException extends Exception {

    /**
     * Creates a new exception with a detail message
     *
     * @param       msg             Detail message
     */
    public SignatureException(String msg) {
        super(msg);
    }

    /**
     * Creates a new exception with a detail message and cause
     *
     * @param       msg             Detail message
     * @param       t               Caught exception
     */
    public SignatureException(String msg, Throwable t) {
        super(msg, t);
    }
}
