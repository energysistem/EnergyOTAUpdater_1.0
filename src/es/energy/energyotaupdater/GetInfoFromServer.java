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
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.util.ArrayList;

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
            //creo los parametros incluyendo dispositivo y rom
            ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("device", android.os.Build.DEVICE.toLowerCase()));
            params.add(new BasicNameValuePair("rom", Utils.getFWVersion()));
            params.add(new BasicNameValuePair("version", Utils.getHWVersion()));

            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(Utils.getServerInfo() + "?" + URLEncodedUtils.format(params, "UTF-8"));
            HttpResponse r = client.execute(get);
            int status = r.getStatusLine().getStatusCode();
            HttpEntity e = r.getEntity();
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
                        json.getString("version"),
                        json.getString("changelog"),
                        json.getString("url"),
                        json.getString("md5"),
                        Utils.parseDate(json.getString("date")));
            } else {
                if (e != null) e.consumeContent();
                error = "Server responded with error " + status;
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            error = e.getMessage();
        }

        return null;
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
