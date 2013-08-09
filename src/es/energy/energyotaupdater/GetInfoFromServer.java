package es.energy.energyotaupdater;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.net.SocketTimeoutException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

/**
 * Created by DFV on 26/07/13.
 */
public class GetInfoFromServer extends AsyncTask<Void,Void,RomInfo> {
    private RomInfoListener callback = null;
    private Context context = null;
    private String error = null;

    public GetInfoFromServer(Context ctx) {
        this(ctx, null);
    }

    public GetInfoFromServer(Context ctx, RomInfoListener callback) {
        this.context = ctx;
        this.callback = callback;
    }

    @Override
    public void onPreExecute() {
        if (callback != null) callback.onStartLoading();
    }

    @Override
    protected RomInfo doInBackground(Void... notused) {
        /*
        if (!Utils.isROMSupported()) {
            error = context.getString(R.string.alert_unsupported_title);
            return null;
        }
        if (!Utils.dataAvailable(context)) {
            error = context.getString(R.string.alert_nodata_title);
            return null;
        }*/

        try {
            //creo los parametros incluyendo dispositivo, versión de firmware y versión de hardware
            ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("device", android.os.Build.DEVICE.toLowerCase().replaceAll(" ","")));
            params.add(new BasicNameValuePair("fwversion", Utils.getFWVersion()));
            params.add(new BasicNameValuePair("hwversion", Utils.getHWVersion()));

            //lanzo la petición mágica al server indicado en el build.prop
            //TODO: cambiar a HttpGet get = new HttpGet(Utils.getServerInfo() + "?" + URLEncodedUtils.format(params, "UTF-8"));
            HttpGet get = new HttpGet("http://192.168.0.10:8080/index.php" + "?" + URLEncodedUtils.format(params, "UTF-8"));
            HttpParams httpParameters = new BasicHttpParams();
            //ponemos un timeout para la conexión de 30 segundos
            int timeoutConnection = 30000;
            HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
            int timeoutSocket = 30000;
            HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
            DefaultHttpClient client = new DefaultHttpClient(httpParameters);
            HttpResponse r = client.execute(get);
            int status = r.getStatusLine().getStatusCode();
            HttpEntity e = r.getEntity();

            //recibo respuesta, si el server está activo, obtengo los datos de la última ROM para el dispositivo (los obtiene el PHP)
            if (status == 200) {
                String data = EntityUtils.toString(e);
                JSONObject json = new JSONObject(data);

                if (json.has("error")) {
                    Log.e("OTA::Fetch", json.getString("error"));
                    error = json.getString("error");
                    return null;
                }

                return new RomInfo(
                        json.getString("rom"),
                        json.getString("fwversion"),
                        json.getString("hwversion"),
                        json.getString("changelog"),
                        json.getString("url"),
                        json.getString("md5"),
                        Date.valueOf(json.getString("date")),
                        json.getString("type"));
            } else {
                if (e != null) e.consumeContent();
                error = "Server responded with error " + status;
                return null;
            }
        } catch (SocketTimeoutException ste)
        {
            ste.printStackTrace();
            error = ste.getMessage();
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            error = e.getMessage();
            return null;
        }
    }

    @Override
    public void onPostExecute(RomInfo result) {
        if (callback != null) {
            if (result != null) callback.onLoaded(result);
            else callback.onError(error);
        }
    }

    public static interface RomInfoListener {
        void onStartLoading();
        void onLoaded(RomInfo info);
        void onError(String err);
    }

}
