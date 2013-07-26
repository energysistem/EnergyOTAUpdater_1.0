package es.energy.energyotaupdater;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import java.io.File;

/**
 * Created by DFV on 25/07/13.
 */
public class Config {
    public static final String OTA_SERVER_INFO = "energy.otaserverinfo";
    public static final String OTA_FW_VERSION = "energy.otafwversion";
    public static final String OTA_SD_PATH_OS_PROP = "energy.sdcardpath.android";
    public static final String OTA_SD_PATH_RECOVERY_PROP = "otaupdater.sdcardpath.recovery";


    public static final int WAKE_TIMEOUT = 30000;

    public static final String DL_PATH = "/" + Utils.getOSSdPath() + "/ENERGY-Updater/download/";
    public static final File DL_PATH_FILE = new File(Config.DL_PATH);

    static {
        if (DL_PATH_FILE.exists()) {
            if (!DL_PATH_FILE.isDirectory()) {
                DL_PATH_FILE.delete();
                DL_PATH_FILE.mkdirs();
            }
        } else {
            DL_PATH_FILE.mkdirs();
        }
    }

    private boolean showNotif = true;
    private boolean ignoredDataWarn = false;

    private int lastVersion = -1;
    private String lastDevice = null;
    private String lastHWVersion = null;

    private int curVersion = -1;
    private String curDevice = null;
    private String curHWVersion = null;

    private RomInfo storedUpdate = null;

    private static final String PREFS_NAME = "prefs";
    private final SharedPreferences PREFS;

    private Config(Context ctx) {
        PREFS = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, 0);

        showNotif = PREFS.getBoolean("showNotif", showNotif);
        ignoredDataWarn = PREFS.getBoolean("ignoredDataWarn", ignoredDataWarn);

        lastVersion = PREFS.getInt("version", lastVersion);
        lastDevice = PREFS.getString("device", lastDevice);
        lastHWVersion = PREFS.getString("curHWVersion", lastHWVersion);

        if (PREFS.contains("info_rom")) {
            storedUpdate = new RomInfo(PREFS.getString("info_rom", null),
                    PREFS.getString("info_version", null),
                    PREFS.getString("info_hwversion",null),
                    PREFS.getString("info_changelog", null),
                    PREFS.getString("info_url", null),
                    PREFS.getString("info_md5", null),
                    Utils.parseDate(PREFS.getString("info_date", null)));
        }

        try {
            curVersion = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }
        curDevice = android.os.Build.DEVICE.toLowerCase();
        curHWVersion = Utils.getHWVersion();

        if (!upToDate()) {
            setIgnoredDataWarn(false);
        }
    }
    private static Config instance = null;
    public static synchronized Config getInstance(Context ctx) {
        if (instance == null) instance = new Config(ctx);
        return instance;
    }

    public boolean getShowNotif() {
        return showNotif;
    }

    public void setShowNotif(boolean showNotif) {
        this.showNotif = showNotif;
        synchronized (PREFS) {
            SharedPreferences.Editor editor = PREFS.edit();
            editor.putBoolean("showNotif", showNotif);
            editor.commit();
        }
    }

    public boolean getIgnoredDataWarn() {
        return ignoredDataWarn;
    }

    public void setIgnoredDataWarn(boolean ignored) {
        this.ignoredDataWarn = ignored;
        synchronized (PREFS) {
            SharedPreferences.Editor editor = PREFS.edit();
            editor.putBoolean("ignoredDataWarn", ignored);
            editor.commit();
        }
    }

    public int getLastVersion() {
        return lastVersion;
    }

    public String getLastDevice() {
        return lastDevice;
    }

    public String getLastHWVersion() {
        return lastHWVersion;
    }

    public void setValuesToCurrent() {
        synchronized (PREFS) {
            SharedPreferences.Editor editor = PREFS.edit();
            editor.putInt("version", curVersion);
            editor.putString("device", curDevice);
            editor.putString("hwversion", curHWVersion);
            editor.commit();
        }
    }

    public boolean upToDate() {
        if (lastDevice == null) return false;
        if (lastHWVersion == null) return false;
        if (curHWVersion == null) return false;
        return curVersion == lastVersion && curDevice.equals(lastDevice) && curHWVersion.equals(curHWVersion);
    }

    public boolean hasStoredUpdate() {
        return storedUpdate != null;
    }

    public RomInfo getStoredUpdate() {
        return storedUpdate;
    }

    public void storeUpdate(RomInfo info) {
        synchronized (PREFS) {
            SharedPreferences.Editor editor = PREFS.edit();
            editor.putString("info_rom", info.romName);
            editor.putString("info_version", info.version);
            editor.putString("info_hwversion", info.hwversion);
            editor.putString("info_changelog", info.changelog);
            editor.putString("info_url", info.downurl);
            editor.putString("info_md5", info.md5);
            editor.putString("info_date", Utils.formatDate(info.date));
            editor.commit();
        }
    }

    public void clearStoredUpdate() {
        synchronized (PREFS) {
            SharedPreferences.Editor editor = PREFS.edit();
            editor.remove("info_rom");
            editor.remove("info_version");
            editor.remove("info_hwversion");
            editor.remove("info_changelog");
            editor.remove("info_url");
            editor.remove("info_md5");
            editor.remove("info_date");
            editor.commit();
        }
    }


}
