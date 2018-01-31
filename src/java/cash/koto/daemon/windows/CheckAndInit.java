package cash.koto.daemon.windows;

import cash.koto.daemon.KotoDaemonConfig;
import cash.koto.daemon.StartupException;
import com.vaklinov.zcashui.Log;

import java.io.FileOutputStream;
import java.io.IOException;
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
            downloadBinary(KotoDaemonConfig.config().getProperty(binaries_koto_cli),"koto-cli.exe");
            downloadBinary(KotoDaemonConfig.config().getProperty(binaries_koto_tx),"koto-tx.exe");
            downloadBinary(KotoDaemonConfig.config().getProperty(binaries_kotod),"kotod.exe");

            downloadBinary(
                    KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_proving_url),
                    KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_proving)
            );
            downloadBinary(
                    KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_verifying_url),
                    KotoDaemonConfig.config().getProperty(KotoDaemonConfig.sprout_verifying)
            );
        } catch (IOException e) {
            throw new StartupException(e);
        }
    }


    private void downloadBinary(String source, String fileName) throws IOException {
        Log.info("downloading binary from \""+source+"\"");
        URL website = new URL(source);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(fileName);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }


}
