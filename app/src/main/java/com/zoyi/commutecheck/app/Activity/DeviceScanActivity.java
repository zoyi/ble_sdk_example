package com.zoyi.commutecheck.app.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import co.zoyi.ble_sdk.Etc.*;
import co.zoyi.ble_sdk.Receiver.*;
import co.zoyi.ble_sdk.Repository.*;
import co.zoyi.ble_sdk.Service.*;

import com.zoyi.commutecheck.app.R;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class DeviceScanActivity extends ApplicationActivity {
  private static final String TAG = "DeviceScanActivity";
  private List<String> targetMacs = new ArrayList<>();
  private boolean mScanning = false;
  private ScannerReceiver scannerReceiver;
  ProgressDialog mProgress;
  Handler mHandler = new Handler();

  public DeviceScanActivity() {
  }

  public void syncAllBtn(View v) {
    syncAllSwitch();
  }

  public void toggleScan(View v) {
    if (!BluetoothService.checkBluetooth(this) && !WifiService.checkWifi(this)) {
      Toast.makeText(this, R.string.cannot_find_available_scanner, Toast.LENGTH_LONG).show();
      finish();
    }

    clearWithUpdateTargetMacs();
    mProgress = ProgressDialog.show(
        this,
        getString(R.string.title_dialog_scanning),
        String.format("tagetMacs: %s, \n%s", getTargetMacs(), getString(R.string.message_dialog_scanning)),
        true,
        true,
        new DialogInterface.OnCancelListener(){
          @Override
          public void onCancel(DialogInterface dialog) {
            stopScanning();
            dialog.dismiss();
            mHandler.removeCallbacksAndMessages(null);
          }});

    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        if (mProgress.isShowing()) {
          stopScanning();
          mProgress.dismiss();
          AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(DeviceScanActivity.this);
          alertDialogBuilder
              .setTitle(getString(R.string.cannot_find_device_title))
              .setMessage(getString(R.string.cannot_find_device_message))
              .setCancelable(true)
              .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
              });
          alertDialogBuilder.create().show();
        }
      }
    }, 20000);



    if (mScanning) {
      stopScanning();
      mHandler.removeCallbacksAndMessages(null);
    } else {
      startScanning();
    }
  }

  public void commuteSuccess(ZoyiSignal record) {
    TextView textView = (TextView)findViewById(R.id.commute_msg);
    String commuteSuccessMsg = getString(R.string.commute_success_message);
    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy년 MM월 dd일 k시 m분");
    stopScanning();
    mHandler.removeCallbacksAndMessages(null);
    mProgress.dismiss();

    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
    alertDialogBuilder
        .setTitle(getString(R.string.commute_success_title))
        .setMessage(String.format("%s %s\n%s", record.getMac(), new DateTime(record.getTs()).toString(fmt.withLocale(Locale.KOREA)), commuteSuccessMsg))
        .setCancelable(true)
        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
            finish();
          }
        });
    alertDialogBuilder.create().show();
  }

  public Boolean isValidateRecord(ZoyiSignal record) {
    if (record instanceof ZoyiBleSignal) {
      if (((ZoyiBleSignal) record).getRssi() >= -70) {
        return true;
      }
    } else if (record instanceof ZoyiWifiSignal) {
      if (((ZoyiWifiSignal) record).getRssi() >= -70) {
        return true;
      }
    }
    return false;
  }

  public void validateRecord(ZoyiSignal record) {
    ZoyiSignal ZoyiSignal = (ZoyiSignal)record;
    Log.v(DeviceScanActivity.class.toString(), ZoyiSignal.toString());
    if (isValidateRecord(record)) {
      commuteSuccess(ZoyiSignal);
      Log.v(DeviceScanActivity.class.toString(), "commuteSuccess");
    }
    Log.d(TAG, record.toString());
  }

  public void updateScanBtn() {
    Button btn = (Button)findViewById(R.id.toggle_scan);
    if (mScanning) {
      btn.setText(R.string.stop_scanning);
    } else {
      btn.setText(R.string.start_scanning);
    }
  }

  public void startScanning() {
    mScanning = true;
    scannerReceiver.startScan(this);
    updateScanBtn();
  }
  public void stopScanning() {
    mScanning = false;
    scannerReceiver.stopScan(this);
    updateScanBtn();
  }

  public List<String> getTargetMacs() {
    EditText text = (EditText)findViewById(R.id.targetMacs);
    String value = text.getText().toString();
    return Arrays.asList(value.split(","));
  }

  public void clearWithUpdateTargetMacs() {
    targetMacs.clear();
    targetMacs.addAll(getTargetMacs());
  }

  public void syncWifiOnOffSwitch() {
    Switch wifiOnOffSwitch = (Switch)  findViewById(R.id.wifi_on_off);
    wifiOnOffSwitch.setChecked(WifiService.checkWifi(this));
  }

  public void handleWifiOnOffSwitch() {
    Switch wifiOnOffSwitch = (Switch)  findViewById(R.id.wifi_on_off);
  }

  public void syncBluetoothOnOffSwitch() {
    Switch bluetoothOnOffSwitch = (Switch)  findViewById(R.id.bluetooth_on_off);
    bluetoothOnOffSwitch.setChecked(BluetoothService.checkBluetooth(this));
  }

  public void handleBluetoothOnOffSwitch() {
    Switch bluetoothOnOffSwitch = (Switch)  findViewById(R.id.bluetooth_on_off);
  }

  public void syncLocationOnOffSwitch() {
    Switch locationOnOffSwitch = (Switch)  findViewById(R.id.location_on_off);
    boolean checked = this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    locationOnOffSwitch.setChecked(checked);
  }

  public void handleLocationOnOffSwitch() {
    Switch locationOnOffSwitch = (Switch)  findViewById(R.id.location_on_off);
  }

  public void syncAllSwitch() {
    syncBluetoothOnOffSwitch();
    syncWifiOnOffSwitch();
    syncLocationOnOffSwitch();
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_device_scan);

    syncAllSwitch();

    handleBluetoothOnOffSwitch();
    handleLocationOnOffSwitch();
    handleWifiOnOffSwitch();

    if (Build.VERSION.SDK_INT >= 18) {
      if (bluetoohSetup() == false) {
        wifiSetup();
      }
    } else {
      // wifi beacon
      wifiSetup();
    }

    if (BluetoothService.checkBluetooth(this)) {
      if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
          requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
        scannerReceiver = new BleReceiver(this, targetMacs, new ScannerReceiverCallback() {
          @Override
          public void onSuccess(ZoyiSignal value) {
            validateRecord(value);
          }
        });
      } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        scannerReceiver = new LegacyBleReceiver(this, targetMacs, new ScannerReceiverCallback() {
          @Override
          public void onSuccess(ZoyiSignal value) {
            validateRecord(value);
          }
        });
      }
    } else {
      scannerReceiver = new WifiReceiver(this, targetMacs, new ScannerReceiverCallback() {
        @Override
        public void onSuccess(ZoyiSignal value) {
          validateRecord(value);
        }
      });
    }
    updateScanBtn();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_ENABLE_BT) {
      if (resultCode == RESULT_CANCELED) {
        // 블루투스는 꺼져있고 와이파이를 키는 경우.
        wifiSetup();
      } else if(resultCode == RESULT_OK) {
        // 나중에 블루투스가 켜진경우.
        scannerReceiver = new BleReceiver(this, targetMacs, new ScannerReceiverCallback() {
          @Override
          public void onSuccess(ZoyiSignal value) {
            validateRecord(value);
          }
        });
      }
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_device_scan, menu);
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

  @Override
  protected void onResume() {
    super.onResume();
    if (mScanning) {
      scannerReceiver.startScan(this);
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mScanning) {
      scannerReceiver.stopScan(this);
    }
  }
}
