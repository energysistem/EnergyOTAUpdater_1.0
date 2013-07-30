package es.energy.energyotaupdater;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.PowerManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;

/**
 * Created by DFV on 30/07/13.
 */
public class Install {

    protected static void installFileDialog(final Context ctx, final File file, final String type) {

        final File RECOVERY_DIR=new File("/cache/recovery");
        final File COMMAND_FILE= new File(RECOVERY_DIR, "command");
        Resources r = ctx.getResources();

        AlertDialog.Builder alert = new AlertDialog.Builder(ctx);
        alert.setTitle("Titulo de alerta de instalación");
        alert.setMessage("ola ke ase? Soy un mensaje!");
        alert.setPositiveButton("instalar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    String name = file.getName();

                    //CODIGO ACTUALIZACIÓN: ROCKCHIP
                    //reiniciamos los directorios por si acaso
                    RECOVERY_DIR.mkdirs();
                    COMMAND_FILE.delete();

                    //creamos un file apuntando al archivo
                    //File update=new File("/sdcard/update.img");

                    //obtenemos su dirección en forma bonita
                    String archivoconruta=file.getCanonicalPath();

                    if(type.equalsIgnoreCase("R"))
                    {
                        instalacionRockchip(COMMAND_FILE,archivoconruta);
                    }
                    else if (type.equalsIgnoreCase("Z"))
                    {
                        instalacionZIPNormal(COMMAND_FILE,archivoconruta);
                    }
                    else
                    {
                        Toast.makeText(ctx,"Actualización no soportada",Toast.LENGTH_LONG).show();
                    }
                    //reiniciamos en recovery
                    ((PowerManager)OTAUpdater.getContext().getSystemService("power")).reboot("recovery");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
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
            //metemos la linea mágica de rockchip en el archivo command
            FileWriter filewriter = new FileWriter(command);
            filewriter.write("--update_package="+archivoconruta);
            filewriter.write("\n");
            filewriter.close();
        }
        catch (Exception e)
        {}
    }
}
