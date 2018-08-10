package bitcoin.walllet.kit.common.keypair;

import java.util.Scanner;

import bitcoin.walllet.kit.common.constant.BitcoinConstants;

/**
 * Generate pretty address starts with specific word. e.g. "BTC".
 * <p>
 * For example:
 * <p>
 * public key: 1BTC48PCwMQMhKr79xgjwdFJFe84iBTrdF private key:
 * Kzgnp4nUukjYuDuJBp9h1hSW4JVigyCAzUXVNvbx2MsLAWUwug5n
 *
 * @author Michael Liao
 */
public class PrettyAddressGenerator {

    static final long NUM_OF_ADDR = 3;

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Enter the prefix (e.g. BTC): ");
            String input = scanner.nextLine();
            if (input.length() == 0) {
                System.err.println("No prefix specified.");
                System.exit(1);
            }
            for (int i = 0; i < input.length(); i++) {
                if (BitcoinConstants.BASE58_CHARS.indexOf(input.charAt(i)) == (-1)) {
                    System.err.println("Invalid char: " + input.charAt(i));
                    System.exit(1);
                }
            }
            String prefix = "1" + input;
            System.out.println("Find address starts with prefix: " + prefix);
            final long step = (long) Math.pow(58, input.length() - 1);
            long n = 0;
            long found = 0;
            for (; ; ) {
                if (n % step == 0) {
                    System.out.print('.');
                }
                n++;
                ECDSAKeyPair keyPair = ECDSAKeyPair.createNewKeyPair();
                String cwif = keyPair.toCompressedWIF();
                String caddr = keyPair.toEncodedCompressedPublicKey();
                if (caddr.startsWith(prefix)) {
                    found++;
                    n = 0;
                    System.out.println("\n" + caddr + " " + cwif);
                    if (found == NUM_OF_ADDR) {
                        break;
                    }
                }
                if (n == Long.MAX_VALUE) {
                    System.out.println("Ooops NO ADDRESS FOUND!");
                    break;
                }
            }
            System.out.println("Found " + found + " addresses.");
        }
    }
}
