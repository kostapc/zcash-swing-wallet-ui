package cash.koto.daemon.windows;

import cash.koto.daemon.KotoDaemonConfig;
import cash.koto.daemon.StartupException;
import com.vaklinov.zcashui.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

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
            downloadBinary(
                    KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_proving_url),
                    KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_proving)
            );
            downloadBinary(
                    KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_verifying_url),
                    KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_verifying)
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


}
