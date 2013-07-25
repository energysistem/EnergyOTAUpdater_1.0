package es.energy.energyotaupdater;

import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

public class OTAUpdater extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otaupdater);

        TextView text_device=(TextView)findViewById(R.id.textView_device);
        text_device.setText(text_device.getText()+" "+ Build.DEVICE);

        TextView text_firmware=(TextView)findViewById(R.id.Firmware);
        text_firmware.setText(text_firmware.getText()+" "+ Build.VERSION.INCREMENTAL);

        TextView text_otaserverinfo=(TextView)findViewById(R.id.textView_otaserverinfo);
        text_otaserverinfo.setText(text_otaserverinfo.getText()+" "+ Utils.getServerInfo());


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.otaupdater, menu);
        return true;
    }
    
}


