package es.energy.energyotaupdater;

import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Created by DFV on 25/07/13.
 */
public class Utils {

    private static String serverinfo=null;

    public static String getServerInfo() {
        if (serverinfo == null) {
            serverinfo = getprop(Config.OTA_SERVER_INFO);
        }
        return serverinfo;
    }

    //busca linea en build.prop y la devuelve (o null si no la encuentra
    private static String getprop(String name) {
        ProcessBuilder pb = new ProcessBuilder("/system/bin/getprop", name);
        pb.redirectErrorStream(true);

        Process p = null;
        InputStream is = null;
        try {
            p = pb.start();
            is = p.getInputStream();
            Scanner scan = new Scanner(is);
            scan.useDelimiter("\n");
            String prop = scan.next();
            if (prop.length() == 0) return null;
            return prop;
        } catch (NoSuchElementException e) {
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try { is.close(); }
                catch (Exception e) { }
            }
        }
        return null;
    }
}
