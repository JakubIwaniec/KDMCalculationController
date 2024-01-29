import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

public class KeyStoreHelper {

    private static final String KEY_ALIAS = "SSHKeyAlias";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final int KEY_SIZE = 2048;

    public static void generateKeyPair(Context context) throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, KEYSTORE_PROVIDER);

        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(KEY_ALIAS)
                .setKeySize(KEY_SIZE)
                .setKeyType(KeyProperties.KEY_ALGORITHM_RSA)
                .setSubject(new X500Principal("CN=" + KEY_ALIAS))
                .setStartDate(Calendar.getInstance().getTime())
                .setEndDate(new Date(System.currentTimeMillis() + 315360000000L))
                .build();

        keyPairGenerator.initialize(spec);
        keyPairGenerator.generateKeyPair();
    }

    public static PrivateKey getPrivateKey(Context context) throws KeyStoreException,
            CertificateException, NoSuchAlgorithmException, IOException,
            UnrecoverableKeyException {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);

        return (RSAPrivateKey) keyStore.getKey(KEY_ALIAS, null);
    }
}
