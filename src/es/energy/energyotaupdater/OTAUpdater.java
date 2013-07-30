package es.energy.energyotaupdater;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.PowerManager;
import android.os.StatFs;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;

public class OTAUpdater extends Activity {

    private DownloadTask dlTask;
    private static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otaupdater);
        context=getApplicationContext();

        //Obtengo el nombre de dispositivo
        TextView text_device=(TextView)findViewById(R.id.textView_device);
        text_device.setText(text_device.getText()+" "+ Build.DEVICE);

        //Obtengo la versión de FW
        /* Por Fecha FW
        TextView text_firmware=(TextView)findViewById(R.id.Firmware);
        text_firmware.setText(text_firmware.getText()+" "+ Build.VERSION.INCREMENTAL);
        */
        //por línea en build.prop
        TextView text_firmware=(TextView)findViewById(R.id.Firmware);
        text_firmware.setText(text_firmware.getText()+" "+ Utils.getFWVersion());

        //Obtengo la dirección del server OTA que se haya indicado en el build.prop
        TextView text_otaserverinfo=(TextView)findViewById(R.id.textView_otaserverinfo);
        text_otaserverinfo.setText(text_otaserverinfo.getText()+" "+ Utils.getServerInfo());

        //comprobación de actualización desde el server, relleno textboxes a lo cutre
        new GetInfoFromServer(getApplicationContext(), new GetInfoFromServer.RomInfoListener() {
            @Override
            public void onStartLoading() { }
            @Override
            public void onLoaded(RomInfo info) {
                if (Utils.isUpdate(info)) {
                    //TODO: temporal
                    Toast.makeText(getApplicationContext(),"fucking yeah!!, hay actualización",Toast.LENGTH_LONG).show();
                    try
                    {
                        TextView text_rom=(TextView)findViewById(R.id.textView_rom);
                        text_rom.setText(text_rom.getText()+" "+ info.romName);

                        TextView text_fwversion=(TextView)findViewById(R.id.textView_fwversion);
                        text_fwversion.setText(text_fwversion.getText()+" "+ info.fwversion);

                        TextView text_hwversion=(TextView)findViewById(R.id.textView_hwversion);
                        text_hwversion.setText(text_hwversion.getText()+" "+ info.hwversion);

                        TextView text_changelog=(TextView)findViewById(R.id.textView_changelog);
                        text_changelog.setText(text_changelog.getText()+" "+ info.changelog);

                        TextView text_downurl=(TextView)findViewById(R.id.textView_downurl);
                        text_downurl.setText(text_downurl.getText()+" "+ info.downurl);

                        TextView text_md5=(TextView)findViewById(R.id.textView_md5);
                        text_md5.setText(text_md5.getText()+" "+ info.md5);

                        TextView text_date=(TextView)findViewById(R.id.textView_date);
                        text_date.setText(text_date.getText()+" "+ info.date);

                        //muestro la ventana de descarga
                        showUpdateDialog(info);
                    }
                    catch (Exception e)
                    {

                    }
                }
            }
            @Override
            public void onError(String error) {

            }
        }).execute();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.otaupdater, menu);
        return true;
    }

    //función para mostrar ventana de descarga
    private void showUpdateDialog(final RomInfo info) {
        //muestro un alert con opción de descargar y cancelar e información sobre la ROM
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Actualización!");
        alert.setMessage("Actualización a:" + info.romName + " , versión:" + info.fwversion);

        alert.setPositiveButton("Descargar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();

                //creo un dialogo de proceso para la descargar con el changelog
                final ProgressDialog progressDialog = new ProgressDialog(OTAUpdater.this);
                progressDialog.setTitle("Descargando");
                progressDialog.setMessage("Changelog: " + info.changelog);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                progressDialog.setProgress(0);

                //creo la tarea de descarga del archivo
                //TODO: modificar nombre de archivo (estudiar coger extensión o procesador de build.prop)

                String extension="";
                if(info.type=="R")
                {
                    extension="img";
                }
                else if(info.type=="Z")
                {
                    extension="zip";
                }

                final File file = new File(Config.DL_PATH_FILE, "update"+extension);
                dlTask = new DownloadTask(progressDialog, info, file);

                //añadimos un botón cancelar a la descarga
                progressDialog.setButton(Dialog.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progressDialog.dismiss();
                        dlTask.cancel(true);
                    }
                });

                dlTask.execute();
            }
        });

        //función/botón cancelar de la alerta de descarga
        alert.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    //tocho función asíncrona para descarga de fichero
    private static class DownloadTask extends AsyncTask<Void, Integer, Integer> {
        private int scale = 1048576;

        private ProgressDialog dialog = null;
        private Context ctx = null;
        private RomInfo info;
        private File destFile;
        private final PowerManager.WakeLock wl;

        private boolean done = false;

        //construcción
        public DownloadTask(ProgressDialog dialog, RomInfo info, File destFile) {
            this.attach(dialog);

            this.info = info;
            this.destFile = destFile;

            PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, OTAUpdater.class.getName());
        }

        public void attach(ProgressDialog dialog) {
            this.dialog = dialog;
            this.ctx = dialog.getContext();
        }

        public void detach() {
            if (this.dialog != null) this.dialog.dismiss();
            this.dialog = null;
            this.ctx = null;
        }

        //fin
        public boolean isDone() {
            return done;
        }

        //obtener info de la rom a descargar
        public RomInfo getRomInfo() {
            return info;
        }

        //antes de empezar se muestra el dialogo y se activa el wakelock
        @Override
        protected void onPreExecute() {
            done = false;
            dialog.show();
            wl.acquire();
        }

        //proceso en segundo plano
        @Override
        protected Integer doInBackground(Void... params) {
            //si el archivo existe y tenemos información sobre el MD5, se comprueba el MD5 para ver
            // si está completo, en dicho caso se devuelve 0 como si la descarga hubiese finalizado,
            //en caso contrario, se borra el archivo y comienza la descargo
            if (destFile.exists() && info.md5!="0" && info.md5 !=null) {
                Log.v("OTA::Download", "Found old zip, checking md5");

                InputStream is = null;
                try {
                    is = new FileInputStream(destFile);
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    byte[] data = new byte[4096];
                    int nRead = -1;
                    while ((nRead = is.read(data)) != -1) {
                        digest.update(data, 0, nRead);
                    }
                    String oldMd5 = Utils.byteArrToStr(digest.digest());
                    Log.v("OTA::Download", "old zip md5: " + oldMd5);
                    if (!info.md5.equalsIgnoreCase(oldMd5)) {
                        destFile.delete();
                    } else {
                        return 0;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    destFile.delete();
                } finally {
                    if (is != null) {
                        try { is.close(); }
                        catch (Exception e) { }
                    }
                }
            }

            InputStream is = null;
            OutputStream os = null;
            try {
                //cogemos la URL de descarga
                URL getUrl = new URL(info.downurl);
                Log.v("OTA::Download", "downloading from: " + getUrl);
                Log.d("OTA::Download", "downloading to: " + destFile.getAbsolutePath());

                //iniciamos conexión
                URLConnection conn = getUrl.openConnection();

                // end do your thing as normal

                //cogemos el tamaño del archivo
                final int lengthOfFile = conn.getContentLength();

                //obtenemos el espacio disponible en memoria
                StatFs stat = new StatFs(Config.DL_PATH);
                long availSpace = ((long) stat.getAvailableBlocks()) * ((long) stat.getBlockSize());

                //si no queda espacio en memoria para el archivo, pues borramos el temporal y
                // salimos con 3 para mostrar luego el mensaje correspondiente
                if (lengthOfFile >= availSpace) {
                    destFile.delete();
                    return 3;
                }

                //preparamos para mostrar el progreso en la barra
                if (lengthOfFile < 10000000) scale = 1024; //if less than 10 mb, scale using kb
                publishProgress(0, lengthOfFile);

                MessageDigest digest = MessageDigest.getInstance("MD5");

                conn.connect();
                is = new BufferedInputStream(conn.getInputStream());
                os = new FileOutputStream(destFile);

                byte[] buf = new byte[4096];
                int nRead = -1;
                int totalRead = 0;
                while ((nRead = is.read(buf)) != -1) {
                    if (this.isCancelled()) break;
                    os.write(buf, 0, nRead);
                    digest.update(buf, 0, nRead);
                    totalRead += nRead;
                    publishProgress(totalRead, lengthOfFile);
                }

                //si se cancela salimos con 2
                if (isCancelled()) {
                    destFile.delete();
                    return 2;
                }

                //comprobamos el MD5, si falla borramos el archivo y salimos con 1
                String dlMd5 = Utils.byteArrToStr(digest.digest());
                Log.v("OTA::Download", "downloaded md5: " + dlMd5);
                if (!info.md5.equalsIgnoreCase(dlMd5)) {
                    //TODO: revisar tema del MD5 return 1;
                    Log.w("OTA::Download", "downloaded md5 doesn't match " + info.md5);
                    destFile.delete();
                    return 1;
                }

                //si acaba bien salimos con 0
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                destFile.delete();
            } finally {
                if (is != null) {
                    try { is.close(); }
                    catch (Exception e) { }
                }
                if (os != null) {
                    try { os.flush(); os.close(); }
                    catch (Exception e) {
                        try{
                            wait(5000);
                            os.flush(); os.close();
                        }
                        catch (InterruptedException ie)
                        {

                        }
                        catch (Exception egeneral)
                        {

                        }
                    }
                }
            }
            return -1;
        }

        //en caso de que se cancele la descarga comprobamos porqué, soltamos el Wakelock, cerramos
        //el diálogo y mostramos la información correspondiente al motivo de cancelación
        @Override
        protected void onCancelled(Integer result) {
            done = true;
            dialog.dismiss();
            wl.release();
            wl.acquire(Config.WAKE_TIMEOUT);

            if (result == null) {
                Toast.makeText(ctx, "Tostada!!!!: error al descargar", Toast.LENGTH_SHORT).show();
                return;
            }

            switch (result) {
                case 0:
                    break;
                case 1:
                    Toast.makeText(ctx, "TOSTADA!!!: No coincide el MD5", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(ctx, "TOSTADA!!!: Descarga interrumpida", Toast.LENGTH_SHORT).show();
                    break;
                case 3:
                    Toast.makeText(ctx, "TOSTADA!!!: No cabe", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(ctx, "TOSTADA!!!: error en general", Toast.LENGTH_SHORT).show();
            }
        }

        //Después de ejecutar, comprobamos si ha saltado error o si ha acabado bien, en ese caso
        //lanzamos instalación
        @Override
        protected void onPostExecute(Integer result) {
            done = true;
            dialog.dismiss();
            wl.release();
            wl.acquire(Config.WAKE_TIMEOUT);

            switch (result) {
                case 0:
                    //TODO: llamar a función instalar instalar ----- ListFilesActivity.installFileDialog(ctx, destFile);
                    Toast.makeText(ctx, "TOSTADA!!!: Descarga finalizada, ahora iríamos a instalar", Toast.LENGTH_LONG).show();
                    Install.installFileDialog(ctx,destFile,info.type);
                    break;
                case 1:
                    Toast.makeText(ctx, "TOSTADA!!!: No coincide el MD5", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    Toast.makeText(ctx, "TOSTADA!!!: Descarga interrumpida", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(ctx, "TOSTADA!!!: error en general", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            if (dialog == null) return;

            if (values[0] == -1) { // i'm not sure yet please hold
                dialog.setIndeterminate(true);
                return;
            }

            dialog.setIndeterminate(false); // make sure dialog is ALWAYS sure for progress

            if (values.length == 0) return;
            dialog.setProgress(values[0] / scale);
            if (values.length == 1) return;
            dialog.setMax(values[1] / scale);
        }
    }

    public static Context getContext()
    {
        return context;
    }
}


