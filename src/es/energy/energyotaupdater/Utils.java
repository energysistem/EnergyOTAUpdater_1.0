package es.energy.energyotaupdater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
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
    public static OTAUpdater.DownloadTask dt;

    //obtener informacion de server desde build.prop, por defecto "updates.energysistem.com"
    public static String getServerInfo() {
        if (serverinfo == null) {
            serverinfo = getprop(Config.OTA_SERVER_INFO);
        }
        if (serverinfo==null)
        {
            serverinfo="http://updates.energysistem.com/ota";
        }
        if (!serverinfo.contains("http://") && !serverinfo.contains("https://"))
        {
            serverinfo="http://"+serverinfo;
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
        if (hwversion == null) {
            hwversion = getprop(Config.OTA_HW_VERSION);
        }
        if (hwversion == null) {
            hwversion = "A";
        }
        return hwversion;
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
        if (info == null) return false;/*
        if (info.fwversion != null) {
            if (getFWVersion() == null || !info.fwversion.equalsIgnoreCase(getFWVersion())) return true;
        }*/
        /*
        if (info.date != null) {
            if (getOtaDate() == null || info.date.after(getOtaDate())) return true;
        }*/
        return true;
    }

    //función que obtiene el PATH de la SD en Android desde el build.prop, por defecto /sdcard
    public static String getOSSdPath() {
        if (cachedOSSdPath == null) {
            cachedOSSdPath = getprop(Config.OTA_SD_PATH_OS_PROP);
            if (cachedOSSdPath == null) {
                cachedOSSdPath = "/sdcard";
            }
        }
        return cachedOSSdPath;
    }

    //función que obtiene el PATH de la SD en el Recovery desde el build.prop, por defecto /sdcard
    public static String getRcvrySdPath() {
        if (cachedRcvrySdPath == null) {
            cachedRcvrySdPath = getprop(Config.OTA_SD_PATH_RECOVERY_PROP);
            Boolean isSDPresent=false;
            File dir = new File("/storage/sdcard1/ENERGY-Updater");
            if(dir.exists() && dir.isDirectory()) {
                isSDPresent=true;
            }
            if (cachedRcvrySdPath == null || isSDPresent) {
                cachedRcvrySdPath = "/sdcard";
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

    public static void showUpdateNotif(Context ctx, RomInfo info) {
        Intent i = new Intent(ctx, OTAUpdater.class);
        i.setAction(OTAUpdater.NOTIF_ACTION);
        info.addToIntent(i);

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx).
        setContentIntent(contentIntent).
        setContentTitle(ctx.getString(R.string.update_available)).
        setContentText(ctx.getString(R.string.new_version_found)).
        setTicker(ctx.getString(R.string.new_version_found)).
        setWhen(System.currentTimeMillis()).
        setSmallIcon(R.drawable.icon_notif);

        nm.notify(28058928, builder.build());
    }

    public static NotificationCompat.Builder showDownloadNotif(Context ctx, RomInfo info, Intent i) {

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(ctx).
            setContentIntent(contentIntent).
            setContentTitle(ctx.getString(R.string.downloading)).
            setContentText(ctx.getString(R.string.download_in_progress)).
            setProgress(0, 0, true).
            setTicker(ctx.getString(R.string.download_in_progress)).
            setSmallIcon(R.drawable.icon_notif);

        nm.notify(28058928, builder.build());
        return builder;
    }


    public static void checkBorrarReiniciar()
    {
        File log=new File(Config.DL_PATH_FILE, "log");
        if(log.exists())
        {
            File list[] = Config.DL_PATH_FILE.listFiles();
            if (list!=null)
            {
                for(int i=0;i<list.length;i++)
                {
                    Log.d("EnergyOTA","Borrando archivo:"+list[i]);
                    list[i].delete();
                }
            }
        }
    }

    public static void guardarLog(File downloadfile)
    {

        try{
            File log = new File(Config.DL_PATH_FILE,"log");
            FileWriter fw=new FileWriter(log,true);
            fw.write(downloadfile.getName());
            fw.close();
        }
        catch (Exception e)
        {
            Log.e("EnergyOTA", "error al guardar el log");
        }

    }
}
