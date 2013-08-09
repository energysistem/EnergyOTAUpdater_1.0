package es.energy.energyotaupdater;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.PowerManager;
import android.os.StatFs;
import android.preference.PreferenceActivity;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
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

    protected static final String NOTIF_ACTION = "es.energy.energyotaupdater.action.NOTIF_ACTION";
    private DownloadTask dlTask;
    private Config cfg;
    private static Context context;
    private boolean dialogFromNotif = false;
    private boolean checkOnResume = false;
    private GetInfoFromServer fetchTask = null;
    private static Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otaupdater);
        context=getApplicationContext();
        cfg = Config.getInstance(getApplicationContext());

        final ProgressBar loading=(ProgressBar)findViewById(R.id.progressBar);

        //enlazo con el botón de la view
        final Button check=(Button)findViewById(R.id.button);
        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //checkForRomUpdates();

                //comprobación de actualización desde el server, relleno textboxes a lo cutre
                new GetInfoFromServer(getApplicationContext(), new GetInfoFromServer.RomInfoListener() {
                    @Override
                    public void onStartLoading() {
                        check.setEnabled(false);
                        check.setText("Buscando...");
                        loading.setVisibility(View.VISIBLE);
                    }
                    @Override
                    public void onLoaded(RomInfo info) {
                        if (Utils.isUpdate(info)) {
                            //TODO: temporal
                            Toast.makeText(getApplicationContext(),"fucking yeah!!, hay actualización",Toast.LENGTH_LONG).show();
                            try
                            {
                                //muestro la ventana de descarga
                                showUpdateDialog(info);
                                check.setEnabled(true);
                                check.setText("Buscar Actualizaciones");
                                loading.setVisibility(View.INVISIBLE);
                            }
                            catch (Exception e)
                            {
                                check.setEnabled(true);
                                check.setText("Buscar Actualizaciones");
                                loading.setVisibility(View.INVISIBLE);
                            }
                        }
                    }
                    @Override
                    public void onError(String error) {
                        check.setEnabled(true);
                        check.setText("Buscar Actualizaciones");
                        loading.setVisibility(View.INVISIBLE);
                    }
                }).execute();
            }
        });
        intent=this.getIntent();

        Object savedInstance = getLastNonConfigurationInstance();

        Log.d("test",String.valueOf(getIntent().getIntExtra("dltask",0)));

        if ((savedInstance != null && savedInstance instanceof DownloadTask) || getIntent().getIntExtra("dltask",0)==1) {
            dialogFromNotif = true;
            dlTask = (DownloadTask) savedInstance;

            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Descargando");
            progressDialog.setMessage("Changelog: " + dlTask.getRomInfo().changelog);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.setProgress(0);
            progressDialog.setButton(Dialog.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    progressDialog.dismiss();
                    dlTask.cancel(true);
                }
            });
            dlTask.attach(progressDialog);
            progressDialog.show();
        } else {
            Intent i = getIntent();
            if (i != null && i.getAction().equals(NOTIF_ACTION)) {
                if (Utils.dataAvailable(getApplicationContext())) {
                    dialogFromNotif = true;
                    showUpdateDialog(RomInfo.fromIntent(i));
                } else {
                    checkOnResume = true;
                }
            } else {
                checkOnResume = true;
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("test","resume:"+String.valueOf(getIntent().getIntExtra("dltask",0)));
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        boolean connected = ni != null && ni.isConnected();
        /*
        if (checkOnResume) {
            if (Utils.dataAvailable(getApplicationContext())) {
                Intent i = getIntent();
                if (i.getAction().equals(NOTIF_ACTION)) {
                    dialogFromNotif = true;
                    showUpdateDialog(RomInfo.fromIntent(i));
                } else {
                    checkForRomUpdates();
                }
                checkOnResume = false;
            }
        }*/
        if (getIntent().getIntExtra("dltask",0)==1) {
            dialogFromNotif = true;

            final ProgressDialog progressDialog = new ProgressDialog(OTAUpdater.this);
            progressDialog.setTitle("Descargando");
            //TODO: sacar la info de algún sitio
            //progressDialog.setMessage("Changelog: " + info.changelog);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setCancelable(false);
            progressDialog.setProgress(0);

            progressDialog.setButton(Dialog.BUTTON_NEUTRAL, "Segundo Plano", new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getIntent().putExtra("dltask",1);
                    Intent main = new Intent(Intent.ACTION_MAIN);
                    main.addCategory(Intent.CATEGORY_HOME);
                    main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(main);

                }
            });
            //añadimos un botón cancelar a la descarga
            progressDialog.setButton(Dialog.BUTTON_NEGATIVE, "Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    progressDialog.dismiss();
                    dlTask.cancel(true);
                }
            });

            dlTask.attach(progressDialog);
            progressDialog.show();
        }
    }

    @Override
    protected void onPause() {
        Log.d("test","pausando");
        if (isFinishing()) {
            if (dlTask != null && !dlTask.isDone()) dlTask.cancel(true);
        }
        if (fetchTask != null) fetchTask.cancel(true);
        super.onPause();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        if (dlTask == null) return null;

        dlTask.detach();
        if (dlTask.isDone()) return null;

        return dlTask;
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
                if(info.type.equalsIgnoreCase("R"))
                {
                    extension=".img";
                }
                else if(info.type.equalsIgnoreCase("Z"))
                {
                    extension=".zip";
                }

                final File file = new File(Config.DL_PATH_FILE, "update"+extension);
                dlTask = new DownloadTask(progressDialog, info, file);


                progressDialog.setButton(Dialog.BUTTON_NEUTRAL, "Segundo Plano", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getIntent().putExtra("dltask",1);
                        Intent main = new Intent(Intent.ACTION_MAIN);
                        main.addCategory(Intent.CATEGORY_HOME);
                        main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(main);

                    }
                });
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
        private NotificationCompat.Builder builder;
        private boolean done = false;
        private int diferencia=0;

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
            builder=Utils.showDownloadNotif(ctx,info,intent);
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
            Utils.showDownloadNotif(ctx,info,intent);
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

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(28058928);
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
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(28058928);
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
            wl.acquire();
            //

            Intent i = new Intent(ctx, OTAUpdater.class);
            i.setAction(OTAUpdater.NOTIF_ACTION);
            PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            builder.setProgress(values[1] / scale, values[0] / scale, false);

            diferencia++;
            if(diferencia==100)
            {
                nm.notify(28058928, builder.build());
                diferencia=0;
            }
        }
    }

    public static Context getContext()
    {
        return context;
    }

    private void checkForRomUpdates() {
        if (fetchTask != null) return;
        fetchTask = new GetInfoFromServer(this, new GetInfoFromServer.RomInfoListener() {
            @Override
            public void onStartLoading() {
            }
            @Override
            public void onLoaded(RomInfo info) {
                fetchTask = null;
                if (info == null) {
                    //availUpdatePref.setSummary(getString(R.string.main_updates_error, "Unknown error"));
                    Toast.makeText(OTAUpdater.this, "error", Toast.LENGTH_SHORT).show();
                } else if (Utils.isUpdate(info)) {
                    showUpdateDialog(info);
                } else {
                    //availUpdatePref.setSummary(R.string.main_updates_none);
                    Toast.makeText(OTAUpdater.this, "no hay actualizaciones", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onError(String error) {
                fetchTask = null;
                //availUpdatePref.setSummary(getString(R.string.main_updates_error, error));
                Toast.makeText(OTAUpdater.this, error, Toast.LENGTH_SHORT).show();
            }
        });
        fetchTask.execute();
    }

}


