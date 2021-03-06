package name.neuhalfen.projects.crypto.bouncycastle.openpgp.example;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BouncyGPG;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BuildDecryptionInputStreamAPI;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.BuildEncryptionOutputStreamAPI;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.callbacks.KeyringConfigCallbacks;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfig;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.keys.keyrings.KeyringConfigs;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.reencryption.FSZipEntityStrategy;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.reencryption.ReencryptExplodedZipSinglethread;
import name.neuhalfen.projects.crypto.bouncycastle.openpgp.reencryption.ZipEntityStrategy;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class MainExplodedSinglethreaded {

  public static void main(String[] args) {
    System.out.println("This program will read an encrypted ZIP file, decrypt it on the fly, and then re-encrypt and write the files from the ZIP into /tmp");
    if (args.length != 7) {
      System.err.format(
          "Usage %s  recipient signWith pubKeyRing secKeyRing secKeyRingPassword sourceFile.zip.gpg destPath\n",
          "java -jar xxx.jar");
      System.exit(-1);
    } else {
      final String recipient = args[0];
      final String signWith = args[1];
      final File pubKeyRing = new File(args[2]);
      final File secKeyRing = new File(args[3]);
      final String secKeyRingPassword = args[4];
      final Path sourceFile = Paths.get(args[5]);
      final File destRootDir = new File(args[6]);

      try {
        BouncyGPG.registerProvider();

        final KeyringConfig keyringConfig = KeyringConfigs.withKeyRingsFromFiles(pubKeyRing,
            secKeyRing, KeyringConfigCallbacks.withPassword(secKeyRingPassword));

        long startTime = System.currentTimeMillis();

        final ZipEntityStrategy zipEntityStrategy = new FSZipEntityStrategy(destRootDir);
        final ReencryptExplodedZipSinglethread reencryptExplodedZip = new ReencryptExplodedZipSinglethread();

        final BuildDecryptionInputStreamAPI.Build decryptionFactory = BouncyGPG
            .decryptAndVerifyStream()
            .withConfig(keyringConfig)
            .andValidateSomeoneSigned();

        final BuildEncryptionOutputStreamAPI.Build encryptionFactory = BouncyGPG
            .encryptToStream()
            .withConfig(keyringConfig)
            .withStrongAlgorithms()
            .toRecipient(recipient)
            .andSignWith(signWith)
            .binaryOutput();

        try (
            final InputStream encryptedSourceZIP = Files.newInputStream(sourceFile);
            final InputStream decryptedSourceZIP = decryptionFactory
                .fromEncryptedInputStream(encryptedSourceZIP)
        ) {
          reencryptExplodedZip
              .explodeAndReencrypt(decryptedSourceZIP, zipEntityStrategy, encryptionFactory);
        }
        long endTime = System.currentTimeMillis();

        System.out.format("Re-Encryption took %.2f s\n", ((double) endTime - startTime) / 1000);
      } catch (Exception e) {
        System.err.format("ERROR: %s", e.getMessage());
        e.printStackTrace();
      }
    }
  }
}
