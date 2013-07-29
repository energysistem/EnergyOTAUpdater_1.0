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
    private static String cachedOSSdPath = null;
    private static String cachedRcvrySdPath = null;

    //obtener informacion de server desde build.prop, por defecto "updates.energysistem.com"
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

    //obtener información de versión de Firmware desde build.prop, por defecto 1.0.0
    public static String getFWVersion() {
        if (fwversion == null) {
            fwversion = getprop(Config.OTA_FW_VERSION);
        }
        if (fwversion == null) {
            fwversion = "1.0.0";
        }
        return fwversion;
    }

    //obtener version de Hardware desde build.prop, por defecto "A"
    public static String getHWVersion() {
        if (hwversion==null) {
            //TODO: coger hwversion de build.prop
        }
        return "A";
    }

    //Función para comprobar si tenemos conexión de red
    public static boolean dataAvailable(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    //busca linea en build.prop y la devuelve (o null si no la encuentra)
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

    //función para analizar fecha
    public static Date parseDate(String date) {
        if (date == null) return null;
        try {
            return new SimpleDateFormat("yyyyMMdd-kkmm").parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    //función para formatear fecha
    public static String formatDate(Date date) {
        if (date == null) return null;
        return new SimpleDateFormat("yyyyMMdd-kkmm").format(date);
    }

    //función que comprueba si una ROM obtenida desde el server es una actualización respecto a la versión instalada
    public static boolean isUpdate(RomInfo info) {
        if (info == null) return false;
        if (info.fwversion != null) {
            if (getFWVersion() == null || !info.fwversion.equalsIgnoreCase(getFWVersion())) return true;
        }
        /*
        if (info.date != null) {
            if (getOtaDate() == null || info.date.after(getOtaDate())) return true;
        }*/
        return false;
    }

    //función que obtiene el PATH de la SD en Android desde el build.prop, por defecto /sdcard
    public static String getOSSdPath() {
        if (cachedOSSdPath == null) {
            cachedOSSdPath = getprop(Config.OTA_SD_PATH_OS_PROP);
            if (cachedOSSdPath == null) {
                cachedOSSdPath = "sdcard";
            }
        }
        return cachedOSSdPath;
    }

    //función que obtiene el PATH de la SD en el Recovery desde el build.prop, por defecto /sdcard
    public static String getRcvrySdPath() {
        if (cachedRcvrySdPath == null) {
            cachedRcvrySdPath = getprop(Config.OTA_SD_PATH_RECOVERY_PROP);
            if (cachedRcvrySdPath == null) {
                cachedRcvrySdPath = "sdcard";
            }
        }
        return cachedRcvrySdPath;
    }

    private static final char[] HEX_DIGITS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
    public static String byteArrToStr(byte[] bytes) {
        StringBuffer str = new StringBuffer();
        for (int q = 0; q < bytes.length; q++) {
            str.append(HEX_DIGITS[(0xF0 & bytes[q]) >>> 4]);
            str.append(HEX_DIGITS[0xF & bytes[q]]);
        }
        return str.toString();
    }
}
