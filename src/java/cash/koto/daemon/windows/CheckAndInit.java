package cash.koto.daemon.windows;

import cash.koto.daemon.KotoDaemonConfig;
import cash.koto.daemon.StartupException;
import com.vaklinov.zcashui.Log;
import com.vaklinov.zcashui.OSUtil;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * 2018-01-21
 *
 * @author KostaPC
 * c0f3.net
 */
public class CheckAndInit {

    private static final String binaries_kotod="koto.binaries.kotod";
    private static final String binaries_koto_tx="koto.binaries.koto-tx";
    private static final String binaries_koto_cli="koto.binaries.koto-cli";


    public void process() {
        try {
            // prepare dirs
            String chainDir = OSUtil.getBlockchainDirectory();
            Log.info("checking and creating conf directory: "+chainDir);
            File file = new File(chainDir);
            if(!file.exists()) {
               if(!file.mkdir()) {
                   throw new StartupException("cannot create koto dir: "+chainDir);
               }
            }

            String confFilePath = chainDir+File.separator+KotoDaemonConfig.config().getProperty(KotoDaemonConfig.koto_conf_file);
            File confFile = new File(confFilePath);
            if(!confFile.exists()) {
                Log.info("writing config file: "+confFilePath);
                Random random = new Random();
                String userName = OSUtil.getUsername()+"_"+random.nextLong();
                String password = OSUtil.getUsername()+"_"+random.nextLong()+"_"+random.nextLong();
                if(!confFile.createNewFile()) {
                    throw new StartupException("cannot create config file: "+confFilePath);
                }
                BufferedWriter writer = new BufferedWriter(new FileWriter(confFile));
                writer.write("rpcuser="+userName+"\n");
                writer.write("rpcpassword="+password+"\n");
                writer.flush();
                writer.close();
            }

            String sproutDir = KotoDaemonConfig.config().getSproutDir();
            File sproutDirFile = new File(sproutDir);
            if(!sproutDirFile.exists()) {
                Log.info("creating ZcashDir for sprout: "+sproutDir);
                if(!sproutDirFile.mkdir()) {
                    throw new StartupException("cannot create sproutDir: "+sproutDir);
                }
            }
            downloadBinary(
                    KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_proving_url),
                    sproutDir+File.pathSeparator+KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_proving)
            );
            verifyFileChecksum(
                    sproutDir+File.pathSeparator+KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_proving),
                    KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_proving_hash)
            );

            downloadBinary(
                    KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_verifying_url),
                    sproutDir+File.pathSeparator+KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_verifying)
            );
            verifyFileChecksum(
                    sproutDir+File.pathSeparator+KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_verifying),
                    KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_verifying_hash)
            );

            downloadBinary(KotoDaemonConfig.config().getProperty(binaries_koto_cli),"koto-cli.exe");
            downloadBinary(KotoDaemonConfig.config().getProperty(binaries_koto_tx),"koto-tx.exe");
            downloadBinary(KotoDaemonConfig.config().getProperty(binaries_kotod),"kotod.exe");

        } catch (IOException e) {
            throw new StartupException(e);
        }
    }


    private void downloadBinary(String source, String fileName) throws IOException {
        Log.info("downloading binary from \""+source+"\"");
        URL website = new URL(source);
        HttpURLConnection connection = (HttpURLConnection) website.openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/4.76");
        InputStream inputStream = connection.getInputStream();
        FileOutputStream fos = new FileOutputStream(fileName);
        try {

            byte[] buf=new byte[8192];
            int bytesread = 0;
            int bytesBuffered = 0;
            while( (bytesread = inputStream.read( buf )) > -1 ) {
                fos.write( buf, 0, bytesread );
                bytesBuffered += bytesread;
                if (bytesBuffered > 1024 * 1024) { //flush after 1MB
                    bytesBuffered = 0;
                    fos.flush();
                }
            }
        } finally {
            if (fos != null) {
                fos.flush();
                fos.close();
            }
            if(inputStream !=null){
                inputStream.close();
            }
        }
    }

    /**
     * Verifies file's SHA256 checksum
     * @param filePath and name of a file that is to be verified
     * @param testChecksum the expected checksum
     * @return true if the expeceted SHA256 checksum matches the file's SHA256 checksum; false otherwise.
     */
    public void verifyFileChecksum(String filePath, String testChecksum)  {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(filePath);

            byte[] data = new byte[1024];
            int read = 0;
            while ((read = fis.read(data)) != -1) {
                sha256.update(data, 0, read);
            }

            byte[] hashBytes = sha256.digest();

            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < hashBytes.length; i++) {
                sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
            }

            String fileHash = sb.toString();

            if(!fileHash.equals(testChecksum)) {
                throw new StartupException("SHA256 hash not match for "+filePath);
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new StartupException(e);
        }
    }


}
