package org.emergent.android.weave.client;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.regex.Pattern;

/**
 * @author Patrick Woodworth
 */
class WeaveCryptoUtil {

  private static final String PROVIDER_NAME = "BC";

  private static final String PROVIDER_CLASS = "org.bouncycastle.jce.provider.BouncyCastleProvider";

  private static final byte[] HMAC_INPUT = WeaveUtil.toAsciiBytes("Sync-AES_256_CBC-HMAC256");

  private static final Pattern ILLEGAL_USERNAME_PATTERN = Pattern.compile("[^A-Z0-9._-]", Pattern.CASE_INSENSITIVE);

  private static final WeaveCryptoUtil sm_instance = new WeaveCryptoUtil();

  static {
    initProvider(PROVIDER_NAME, PROVIDER_CLASS);
  }

  private WeaveCryptoUtil() {
  }

  public static WeaveCryptoUtil getInstance() {
    return sm_instance;
  }

  public String legalizeUsername(String friendlyUsername) {
    try {
      if (!ILLEGAL_USERNAME_PATTERN.matcher(friendlyUsername).find())
          return friendlyUsername;

      MessageDigest digest = MessageDigest.getInstance("SHA1");
      digest.update(WeaveUtil.toAsciiBytes(friendlyUsername.toLowerCase()));
      byte[] baseEncodedBytes = Base32.encode(digest.digest());
      return WeaveUtil.toAsciiString(baseEncodedBytes);
    } catch (GeneralSecurityException e) {
      throw new Error(e);
    }
  }

  public RSAPublicKey readCertificatePubKey(String base64EncodedCert) throws GeneralSecurityException {
    byte[] certBytes = Base64.decode(base64EncodedCert);
//    X509EncodedKeySpec keySpec = new X509EncodedKeySpec(certBytes);
//    KeyFactory certFact = KeyFactory.getInstance("RSA", PROVIDER_NAME);
//    return (RSAPublicKey)certFact.generatePublic(keySpec);
    return (RSAPublicKey)readCertificate(certBytes);
  }

  public X509Certificate readCertificate(byte[] certBytes) throws GeneralSecurityException {
    CertificateFactory certFact = getCertificateFactoryInstance("X.509");
    return (X509Certificate)certFact.generateCertificate(new ByteArrayInputStream(certBytes));
  }

  public byte[] readPrivateKeyToPKCSBytes(char[] encpass, String salt, String iv, String keyData)
      throws GeneralSecurityException
  {
    Key key = getKeyDecryptionKey(encpass, Base64.decode(salt));
    Cipher cipher = getCipherInstance("AES/CBC/PKCS5PADDING");
    cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(Base64.decode(iv)));
    return cipher.doFinal(Base64.decode(keyData));
  }

  public RSAPrivateKey decodePrivateKeyFromPKCSBytes(byte[] keySpecBytes) throws GeneralSecurityException {
    KeySpec keySpec = new PKCS8EncodedKeySpec(keySpecBytes);
    KeyFactory keyFact = getKeyFactoryInstance("RSA");
    return (RSAPrivateKey)keyFact.generatePrivate(keySpec);
  }

  public byte[] decrypt(Key secKey, String ciphertext, String iv, String hmac) throws GeneralSecurityException {
//    checkMac(secKey, ciphertext, hmac);
    byte[] ciphertextbytes = Base64.decode(ciphertext);
    byte[] ivBytes = Base64.decode(iv);
    Cipher cipher = getCipherInstance("AES/CBC/PKCS5PADDING");
    cipher.init(Cipher.DECRYPT_MODE, secKey, new IvParameterSpec(ivBytes));
    return cipher.doFinal(ciphertextbytes);
  }

  public String decrypt(Key secKey, Key hmacKey, String ciphertext, String iv, String hmac) throws GeneralSecurityException {
    checkMac(hmacKey, ciphertext, hmac);
    byte[] ciphertextbytes = Base64.decode(ciphertext);
    byte[] ivBytes = Base64.decode(iv);
    Cipher cipher = getCipherInstance("AES/CBC/PKCS5PADDING");
    cipher.init(Cipher.DECRYPT_MODE, secKey, new IvParameterSpec(ivBytes));
//    System.err.println("lengths : "  + " iv=" + ivBytes.length + " ivstr=" + iv.length() + " ; hmacstr=" + hmac.length());
    return WeaveUtil.toUtf8String(cipher.doFinal(ciphertextbytes));
  }

  public String encrypt(Key secKey, Key hmacKey, String plaintext, String iv) throws GeneralSecurityException {
//    byte[] ivBytes = new byte[16];
//    mSecureRandom.nextBytes(ivBytes);
    byte[] ivBytes = Base64.decode(iv);
    Cipher cipher = getCipherInstance("AES/CBC/PKCS5PADDING");
    cipher.init(Cipher.ENCRYPT_MODE, secKey, new IvParameterSpec(ivBytes));
    byte[] plaintextbytes = WeaveUtil.toUtf8Bytes(plaintext);
    byte[] ciphertextbytes = cipher.doFinal(plaintextbytes);
    return WeaveUtil.toAsciiString(Base64.encode(ciphertextbytes));
  }

  public Key unwrapSecretKey(RSAPrivateKey privKey, String wrapped) throws GeneralSecurityException {
    byte[] wrappedBytes = Base64.decode(wrapped);
    Cipher cipher = getCipherInstance("RSA/ECB/PKCS1Padding");
    cipher.init(Cipher.UNWRAP_MODE, privKey);
    return cipher.unwrap(wrappedBytes, "AES", Cipher.SECRET_KEY);
  }

  protected Key getKeyDecryptionKey(char[] secret, byte[] salt) throws GeneralSecurityException {
    byte[] keyBytes = derivePKCS5S2(secret, salt);
    return new SecretKeySpec(keyBytes, "AES");
  }

  private byte[] encrypt(Key secKey, String plaintext, String iv) throws GeneralSecurityException {
    byte[] plaintextbytes = Base64.decode(plaintext);
    return encrypt(secKey, plaintextbytes, iv);
  }

  private byte[] encrypt(Key secKey, byte[] plaintext, String iv) throws GeneralSecurityException {
    Cipher cipher = getCipherInstance("AES/CBC/PKCS5PADDING");
    cipher.init(Cipher.ENCRYPT_MODE, secKey, new IvParameterSpec(Base64.decode(iv)));
    return cipher.doFinal(plaintext);
  }

  String createMac(Key secKey, String ciphertext) throws GeneralSecurityException {
    Mac mac = getMacInstance("HMACSHA256");
//    mac.init(new SecretKeySpec(Base64.encode(secKey.getEncoded()), "AES"));
    mac.init(secKey);
    byte[] hmacBytes = mac.doFinal(WeaveUtil.toAsciiBytes(ciphertext));
    return WeaveUtil.toAsciiString(Hex.encode(hmacBytes));
  }

  void checkMac(Key secKey, String ciphertext, String hmac) throws GeneralSecurityException {
    String hmac2 = createMac(secKey, ciphertext);
    if (!hmac.equalsIgnoreCase(hmac2))
      throw new GeneralSecurityException("mac failed");
  }

  protected static boolean initProvider(String providerName, String className) {
    try {
      Provider provider = Security.getProvider(providerName);
      if (provider == null) {
        Class clazz = Class.forName(className);
        provider = (Provider)clazz.newInstance();
        Security.addProvider(provider);
      }
      return true;
    } catch (Throwable ignored) {
    }
    return false;
  }

  public static byte[] deriveSyncKey(byte[] secretBytes, String username) throws GeneralSecurityException {
    int keySizeInBytes = 256 / 8;
    Mac hMac = Mac.getInstance("HMACSHA256");

    byte[] state = new byte[hMac.getMacLength()];
    SecretKeySpec param = new SecretKeySpec(secretBytes, "SHA256");
    hMac.init(param);
    hMac.update(HMAC_INPUT);
    hMac.update(WeaveUtil.toAsciiBytes(username));
    hMac.update((byte) 0x1);
    hMac.doFinal(state, 0);
    byte[] retval = new byte[keySizeInBytes];
    System.arraycopy(state, 0, retval, 0, keySizeInBytes);
    return retval;
  }

  public static byte[] deriveSyncHmacKey(byte[] secretBytes, byte[] bkbytes, String username) throws GeneralSecurityException {
    int keySizeInBytes = 256 / 8;
    Mac hMac = Mac.getInstance("HMACSHA256");

    byte[] state = new byte[hMac.getMacLength()];
    SecretKeySpec param = new SecretKeySpec(secretBytes, "SHA256");
    hMac.init(param);
    hMac.update(bkbytes);
//    hMac.update(WeaveUtil.toAsciiBytes(bkstr));
    hMac.update(HMAC_INPUT);
    hMac.update(WeaveUtil.toAsciiBytes(username));
    hMac.update((byte) 0x2);
    hMac.doFinal(state, 0);
    byte[] retval = new byte[keySizeInBytes];
    System.arraycopy(state, 0, retval, 0, keySizeInBytes);
    return retval;
  }

  /**
   * This code basically inlines usage of the BouncyCastle private API and is equivalent to the following code.
   * <pre>
   * {@code
   * PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();
   * generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(secret), salt, 4096);
   * CipherParameters keyParam = generator.generateDerivedParameters(256);
   * return ((KeyParameter)keyParam).getKey();
   * }
   * </pre>
   */
  private static byte[] derivePKCS5S2(char[] secret, byte[] salt) throws GeneralSecurityException {
    byte[] secretBytes = passwordPKCS5ToBytes(secret);
    int keySizeInBytes = 256 / 8;
    final int iterations = 4096;
    Mac hMac = Mac.getInstance("HMACSHA1");
    int hLen = hMac.getMacLength();
    int l = (keySizeInBytes + hLen - 1) / hLen;
    byte[] iBuf = new byte[4];
    byte[] dKey = new byte[l * hLen];
    for (int i = 1; i <= l; i++) {
      intToOctet(iBuf, i);
      derivePKCS5S2Helper(hMac, secretBytes, salt, iterations, iBuf, dKey, (i - 1) * hLen);
    }
    byte[] retval = new byte[keySizeInBytes];
    System.arraycopy(dKey, 0, retval, 0, keySizeInBytes);
    return retval;
  }

  private static byte[] passwordPKCS5ToBytes(char[] password) {
    byte[] bytes = new byte[password.length];
    for (int ii = 0; ii != bytes.length; ii++) {
      bytes[ii] = (byte)password[ii];
    }
    return bytes;
  }

  private static void derivePKCS5S2Helper(Mac hMac, byte[] P, byte[] S, int c, byte[] iBuf, byte[] out, int outOff)
      throws GeneralSecurityException
  {
    byte[] state = new byte[hMac.getMacLength()];
    SecretKeySpec param = new SecretKeySpec(P, "SHA1");
    hMac.init(param);
    if (S != null) {
      hMac.update(S, 0, S.length);
    }
    hMac.update(iBuf, 0, iBuf.length);
    hMac.doFinal(state, 0);
    System.arraycopy(state, 0, out, outOff, state.length);
    if (c == 0) {
      throw new IllegalArgumentException("iteration count must be at least 1.");
    }
    for (int count = 1; count < c; count++) {
      hMac.init(param);
      hMac.update(state, 0, state.length);
      hMac.doFinal(state, 0);
      for (int j = 0; j != state.length; j++) {
        out[outOff + j] ^= state[j];
      }
    }
  }

  private static void intToOctet(byte[] buf, int i) {
    buf[0] = (byte)(i >>> 24);
    buf[1] = (byte)(i >>> 16);
    buf[2] = (byte)(i >>> 8);
    buf[3] = (byte)i;
  }

  private static CertificateFactory getCertificateFactoryInstance(String algo) throws NoSuchProviderException, CertificateException {
//    return CertificateFactory.getInstance(algo, PROVIDER_NAME);
    return CertificateFactory.getInstance(algo);
  }

  private static KeyFactory getKeyFactoryInstance(String algo) throws NoSuchProviderException, NoSuchAlgorithmException {
//    return KeyFactory.getInstance(algo, PROVIDER_NAME);
    return KeyFactory.getInstance(algo);
  }

  private static Cipher getCipherInstance(String algo) throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
//    return Cipher.getInstance(algo, PROVIDER_NAME);
    return Cipher.getInstance(algo);
  }

  private static Mac getMacInstance(String algo) throws NoSuchProviderException, NoSuchAlgorithmException {
//    return Mac.getInstance(algo, PROVIDER_NAME);
    return Mac.getInstance(algo);
  }
}
