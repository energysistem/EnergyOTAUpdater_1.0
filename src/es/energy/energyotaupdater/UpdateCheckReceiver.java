package es.energy.energyotaupdater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import es.energy.energyotaupdater.GetInfoFromServer.RomInfoListener;
/**
 * Created by DFV on 26/07/13.
 */
//Clase para comprobar actualizaciones
public class UpdateCheckReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent)
    {
        final Config cfg = Config.getInstance(context.getApplicationContext());

        //impedimos que el tablet entre en bloqueo obteniendo el wakelock
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, UpdateCheckReceiver.class.getName());
        wl.acquire();

        //lanzamos la peticion al server
        new GetInfoFromServer(context, new RomInfoListener() {
            @Override
            public void onStartLoading() { }
            @Override
            public void onLoaded(RomInfo info) {
                //si la rom obtenida es más actual
                if (Utils.isUpdate(info)) {
                    //TODO: temporal
                    Toast.makeText(context,"fucking yeah!!, hay actualización",Toast.LENGTH_LONG).show();

                    cfg.storeUpdate(info);
                    //si están activadas las notificaciones, lanzamos la de actualización
                    if (cfg.getShowNotif()) {
                        //Utils.showUpdateNotif(context, info);
                    } else {
                        Log.v("OTA::Receiver", "found update, notif not shown");
                    }
                } else {
                    cfg.clearStoredUpdate();
                }
                //soltamos el wakelock
                wl.release();
            }
            @Override
            public void onError(String error) {
                wl.release();
            }
        }).execute();
    }
}
