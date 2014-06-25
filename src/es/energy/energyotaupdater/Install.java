package es.energy.energyotaupdater;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.net.URI;

/**
 * Created by DFV on 30/07/13.
 */
public class Install {

    protected static void installFileDialog(final Context ctx, final File file, final String type) {

        final File RECOVERY_DIR=new File("/cache/recovery");
        final File COMMAND_FILE= new File(RECOVERY_DIR, "command");
        Resources r = ctx.getResources();

        AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
        alert.setTitle(ctx.getString(R.string.install_update));
        alert.setMessage(ctx.getString(R.string.install_text));
        AlertDialog.Builder builder = alert.setPositiveButton(ctx.getString(R.string.install), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    Utils.guardarLog(file);
                    String name = file.getName();
                    //reiniciamos los directorios por si acaso
                    RECOVERY_DIR.mkdirs();
                    COMMAND_FILE.delete();

                    //creamos un file apuntando al archivo
                    //File update=new File("/sdcard/update.img");


                    if (type.equalsIgnoreCase("R")) {
                        //solución para recovery con path modificado a la sd
                        String archivoconruta = Utils.getRcvrySdPath() + "/ENERGY-Updater/download/update.img";
                        instalacionRockchip(COMMAND_FILE, archivoconruta);
                        ((PowerManager) OTAUpdater.getContext().getSystemService("power")).reboot("recovery");
                    } else if (type.equalsIgnoreCase("Z")) {
                        //solución para recovery con path modificado a la sd
                        String archivoconruta = Utils.getRcvrySdPath() + "/ENERGY-Updater/download/update.zip";
                        instalacionZIPNormal(COMMAND_FILE, archivoconruta);
                        ((PowerManager) OTAUpdater.getContext().getSystemService("power")).reboot("recovery");
                    } else {
                        Toast.makeText(ctx, ctx.getString(R.string.update_not_supported), Toast.LENGTH_LONG).show();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        alert.setNegativeButton(ctx.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.create().show();
    }

    private static void instalacionRockchip(File command, String archivoconruta){
        try
        {
            //metemos la linea mágica de rockchip en el archivo command
            FileWriter filewriter = new FileWriter(command);
            filewriter.write("--update_rkimage="+archivoconruta);
            filewriter.write("\n");
            filewriter.close();
        }
        catch (Exception e)
        {}
    }

    private static void instalacionZIPNormal(File command, String archivoconruta){
        try
        {
            //metemos la linea mágica genérica en el archivo command
            FileWriter filewriter = new FileWriter(command);
            filewriter.write("--update_package="+archivoconruta);
            filewriter.write("\n");
            filewriter.close();
        }
        catch (Exception e)
        {}
    }

}



