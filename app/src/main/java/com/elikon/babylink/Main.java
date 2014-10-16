package com.elikon.babylink;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class Main extends Activity {
    private Device device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BabyLinkApplication.DeviceType deviceType = ((BabyLinkApplication) this.getApplication()).getDeviceType();
        if ( deviceType == BabyLinkApplication.DeviceType.NONE ) {
            this.setContentView(R.layout.activity_none_device);
        } else if (deviceType == BabyLinkApplication.DeviceType.PARENT) {
            this.setContentView(R.layout.activity_parent_device);
        } else {
            this.setContentView(R.layout.activity_child_device);
        }
        ActionBar actionBar = getActionBar();
        ColorDrawable cd = new ColorDrawable();
        cd.setColor(Color.rgb(0,153,57));
        actionBar.setBackgroundDrawable(cd);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            BabyLinkApplication.DeviceType deviceType = ((BabyLinkApplication) this.getApplication()).getDeviceType();

            if ( deviceType == BabyLinkApplication.DeviceType.NONE ) {
                this.setTitle(getString(R.string.select_device));
                getMenuInflater().inflate(R.menu.none_device, menu);
            } else if (deviceType == BabyLinkApplication.DeviceType.PARENT) {
                setTitle(getString(R.string.parent_device));
                getMenuInflater().inflate(R.menu.parent_device, menu);
                if (device != null) {
                    device.tearDown();
                }
                device = new ParentDevice(getApplicationContext(), this, (TextView) findViewById(R.id.statusViewChild));
            } else {
                setTitle(getString(R.string.child_device));
                getMenuInflater().inflate(R.menu.child_device, menu);
                if (device != null) {
                    device.tearDown();
                }
                device = new ChildDevice(this, (TextView) findViewById(R.id.statusViewParent));

            }
        } catch (Exception e) {
            Log.appendLog(this, e.getMessage() + e.getLocalizedMessage() + e.toString());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.parent_device) {
            ((BabyLinkApplication) this.getApplication()).setDeviceType(BabyLinkApplication.DeviceType.PARENT);
            finish();
            startActivity(getIntent());
        } else if (id == R.id.child_device) {
            ((BabyLinkApplication) this.getApplication()).setDeviceType(BabyLinkApplication.DeviceType.CHILD);
            finish();
            startActivity(getIntent());
        } else if (id == R.id.connection_settings) {
            Intent intent = new Intent(this, ConnectionTypeSettings.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        if ( device != null) {
            device.tearDown();
        }
        super.onDestroy();
    }
}
