package es.energy.energyotaupdater;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Created by DFV on 25/07/13.
 */
public class Utils {

    private static String serverinfo=null;
    private static String fwversion=null;
    private static String hwversion=null;

    public static String getServerInfo() {
        if (serverinfo == null) {
            serverinfo = getprop(Config.OTA_SERVER_INFO);
        }
        if (serverinfo==null)
        {
            serverinfo="updates.energysistem.com";
        }
        return serverinfo;
    }

    public static String getFWVersion() {
        if (hwversion == null) {
            hwversion = getprop(Config.OTA_FW_VERSION);
        }
        return fwversion;
    }

    public static String getHWVersion() {
        if (hwversion==null) {
            //TODO: coger hwversion de build.prop
        }
        return "A";
    }

    public static boolean dataAvailable(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
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

    public static Date parseDate(String date) {
        if (date == null) return null;
        try {
            return new SimpleDateFormat("yyyyMMdd-kkmm").parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String formatDate(Date date) {
        if (date == null) return null;
        return new SimpleDateFormat("yyyyMMdd-kkmm").format(date);
    }
}
