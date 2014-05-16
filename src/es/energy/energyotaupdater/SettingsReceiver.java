package es.energy.energyotaupdater;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;

/**
 * Created by DFV on 23/08/13.
 */
public class SettingsReceiver extends Activity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Intent intent =  getPackageManager().getLaunchIntentForPackage("es.energy.energyotaupdater");
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("es.energy.energyotaupdater", "es.energy.energyotaupdater.OTAUpdater"));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        this.finish();
    }
}
