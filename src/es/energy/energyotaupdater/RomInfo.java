package es.energy.energyotaupdater;

import android.content.Intent;

import java.util.Date;

/**
 * Created by DFV on 26/07/13.
 */
public class RomInfo {
    public String romName;
    public String version;
    public String hwversion;
    public String changelog;
    public String downurl;
    public String md5;
    public Date date;

    public RomInfo(String romName, String version, String hwversion, String changelog, String downurl, String md5, Date date) {
        this.romName = romName;
        this.version = version;
        this.hwversion = hwversion;
        this.changelog = changelog;
        this.downurl = downurl;
        this.md5 = md5;
        this.date = date;
    }
    public static RomInfo fromIntent(Intent i) {
        return new RomInfo(
                i.getStringExtra("info_rom"),
                i.getStringExtra("info_version"),
                i.getStringExtra("info_hwversion"),
                i.getStringExtra("info_changelog"),
                i.getStringExtra("info_downurl"),
                i.getStringExtra("info_md5"),
                Utils.parseDate(i.getStringExtra("info_date")));
    }

    public void addToIntent(Intent i) {
        i.putExtra("info_rom", romName);
        i.putExtra("info_version", version);
        i.putExtra("info_hwversion", hwversion);
        i.putExtra("info_changelog", changelog);
        i.putExtra("info_downurl", downurl);
        i.putExtra("info_md5", md5);
        i.putExtra("info_date", Utils.formatDate(date));
    }

}
