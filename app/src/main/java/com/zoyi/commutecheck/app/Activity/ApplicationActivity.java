package com.zoyi.commutecheck.app.Activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import co.zoyi.ble_sdk.Service.*;
import com.zoyi.commutecheck.app.R;


public class ApplicationActivity extends Activity {
  protected final static int REQUEST_ENABLE_BT = 987654321;

  public Boolean bluetoohSetup() {
    if (!BluetoothService.hasBLE(this)) {
      Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
      return false;
    }

    if (!BluetoothService.checkBluetooth(this)) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
    return true;
  }

  public Boolean wifiSetup(){
    WifiManager wifiManager=(WifiManager)getSystemService(this.WIFI_SERVICE);
    if (!WifiService.checkWifi(this)) {
      Toast.makeText(this, R.string.enable_wifi, Toast.LENGTH_LONG).show();
      wifiManager.setWifiEnabled(true);
    }

    return true;
  }

  public void openDeviceScanActivity() {
    Intent intent = new Intent(this, DeviceScanActivity.class);
    startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_application, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }
}
