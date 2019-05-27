package com.zoyi.commutecheck.app.Activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import java.util.*;


public class DeviceScanActivity extends ApplicationActivity {
  private static final String TAG = "DeviceScanActivity";
  private List<String> targetMacs = new ArrayList<>();
  private Map<String, List<Integer>> monitoringTargetMacRssi = new HashMap<>();
  private MonitoringTargetMacAdapter monitoringTargetMacAdapter;
  private boolean mScanning = false;
  private ScannerReceiver wifiReceiver;
  private ScannerReceiver bleReceiver;
  ProgressDialog mProgress;
  Handler mHandler = new Handler();


  public DeviceScanActivity() {
  }

  public void syncAllBtn(View v) {
    syncAllSwitch();
  }

  public void createAndRegisterWifiReceiver() {
    if (wifiReceiver != null) {
      wifiReceiver.stopScan(this);
      wifiReceiver = null;
    }
    wifiReceiver = new WifiReceiver(this, targetMacs, new ScannerReceiverCallback() {
      @Override
      public void onSuccess(ZoyiSignal value) {
        validateRecord(value);
      }
    });
  }

  public void createAndRegisterBleReceiver() {
    if (bleReceiver != null) {
      bleReceiver.stopScan(this);
      bleReceiver = null;
    }
    // latest versions
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      bleReceiver = new BleReceiver(this, targetMacs, new ScannerReceiverCallback() {
        @Override
        public void onSuccess(ZoyiSignal value) {
          validateRecord(value);
        }
      });
    } // old versions
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      bleReceiver = new LegacyBleReceiver(this, targetMacs, new ScannerReceiverCallback() {
        @Override
        public void onSuccess(ZoyiSignal value) {
          validateRecord(value);
        }
      });
    }
  }

  public ProgressDialog showCommuteProgressDialog(final ScannerReceiver receiver, final Context context) {
    mProgress =  ProgressDialog.show(
        this,
        getString(R.string.title_dialog_scanning),
        String.format("tagetMacs: %s, \n%s", getTargetMacs(), getString(R.string.message_dialog_scanning)),
        true,
        true,
        new DialogInterface.OnCancelListener(){
          @Override
          public void onCancel(DialogInterface dialog) {
            receiver.stopScan(context);
            dialog.dismiss();
            mHandler.removeCallbacksAndMessages(null);
          }});

    mHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        if (mProgress.isShowing()) {
          receiver.stopScan(context);
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

    return mProgress;
  }

  public void activateWifiReceiver(View v) {
    clearWithUpdateTargetMacs();
    createAndRegisterWifiReceiver();
    wifiReceiver.startScan(this);
//    showCommuteProgressDialog(wifiReceiver, this);
  }

  public void activateBleReceiver(View v) {
    clearWithUpdateTargetMacs();
    createAndRegisterBleReceiver();
    bleReceiver.startScan(this);
//    showCommuteProgressDialog(bleReceiver, this);
  }

  public void commuteSuccess(ZoyiSignal record) {
    String commuteSuccessMsg = getString(R.string.commute_success_message);
    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy년 MM월 dd일 k시 m분");
    if (record instanceof ZoyiBleSignal) {
      clearHandlerAndDialogStopReceiver(bleReceiver, this);
    } else if (record instanceof ZoyiWifiSignal) {
      clearHandlerAndDialogStopReceiver(wifiReceiver, this);
    }

    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
    alertDialogBuilder
        .setTitle(getString(R.string.commute_success_title))
        .setMessage(String.format("%s %s\n%s", record.getMac(), new DateTime(record.getTs()).toString(fmt.withLocale(Locale.KOREA)), commuteSuccessMsg))
        .setCancelable(true)
        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialogInterface, int i) {
          }
        });
    alertDialogBuilder.create().show();
  }

  public Boolean isValidateRecord(ZoyiSignal record) {
    if (record instanceof ZoyiBleSignal) {
      if (((ZoyiBleSignal) record).getRssi() >= -65) {
        return true;
      }
    } else if (record instanceof ZoyiWifiSignal) {
      if (((ZoyiWifiSignal) record).getRssi() >= -65) {
        return true;
      }
    }
    return false;
  }

  public void addMonitoringTargetMacRssi(ZoyiSignal record) {
    List<Integer> rssis = monitoringTargetMacRssi.get(record.getMac());
    if (rssis == null) {
      rssis = new ArrayList<>();
    }
    rssis.add(record.getRssi());
    monitoringTargetMacRssi.put(record.getMac(), rssis);
    monitoringTargetMacAdapter.setDataSet(monitoringTargetMacRssi);
    monitoringTargetMacAdapter.notifyDataSetChanged();
  }

  public void validateRecord(ZoyiSignal record) {
    Log.v(DeviceScanActivity.class.toString(), record.toString());
    addMonitoringTargetMacRssi(record);
    if (isValidateRecord(record)) {
//      commuteSuccess(record);
      Log.v(DeviceScanActivity.class.toString(), "commuteSuccess");
    } else {
      Log.d(DeviceScanActivity.class.toString(), "rssi to weak");
    }
  }

  public List<String> getTargetMacs() {
    EditText text = (EditText)findViewById(R.id.target_macs);
    String value = text.getText().toString();
    List<String> result = new ArrayList<>();
    for (String token : value.split(",")) {
      if (!token.equals("")) {
        result.add(token);
      }
    }
    return result;
  }

  public void clearHandlerAndDialogStopReceiver(final ScannerReceiver receiver, final Context context) {
    receiver.stopScan(context);
    mHandler.removeCallbacksAndMessages(null);
    mProgress.dismiss();
  }

  public void clearWithUpdateTargetMacs() {
    targetMacs.clear();
    targetMacs.addAll(getTargetMacs());
    monitoringTargetMacRssi.clear();
  }

  public void syncWifiOnOffSwitch() {
    Switch wifiOnOffSwitch = (Switch)  findViewById(R.id.wifi_on_off);
    wifiOnOffSwitch.setChecked(WifiService.checkWifi(this));
  }

  public void handleWifiOnOffSwitch() {
    Switch wifiOnOffSwitch = (Switch)  findViewById(R.id.wifi_on_off);
    wifiOnOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        wifiEnabled(isChecked);
      }
    });
  }

  public void syncBluetoothOnOffSwitch() {
    Switch bluetoothOnOffSwitch = (Switch)  findViewById(R.id.bluetooth_on_off);
    bluetoothOnOffSwitch.setChecked(BluetoothService.checkBluetooth(this));
  }

  public void handleBluetoothOnOffSwitch() {
    Switch bluetoothOnOffSwitch = (Switch)  findViewById(R.id.bluetooth_on_off);
    bluetoothOnOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        bluetoothEnabled(isChecked);
      }
    });
  }

  public void syncLocationOnOffSwitch() {
    Switch locationOnOffSwitch = (Switch)  findViewById(R.id.location_on_off);
    boolean checked = this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    locationOnOffSwitch.setChecked(checked);
    if (checked) {
      locationOnOffSwitch.setEnabled(false);
      locationOnOffSwitch.setClickable(false);
    }
  }

  public void handleLocationOnOffSwitch() {
    Switch locationOnOffSwitch = (Switch)  findViewById(R.id.location_on_off);
    locationOnOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
          requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
          buttonView.setEnabled(false);
          buttonView.setClickable(false);
        }
      }
    });
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
    RecyclerView recyclerView = findViewById(R.id.monitoring_target_mac_recycler) ;
    recyclerView.setLayoutManager(new LinearLayoutManager(this)) ;

    // 리사이클러뷰에 SimpleTextAdapter 객체 지정.
    monitoringTargetMacAdapter = new MonitoringTargetMacAdapter(monitoringTargetMacRssi) ;
    recyclerView.setAdapter(monitoringTargetMacAdapter) ;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == REQUEST_ENABLE_BT) {
      syncBluetoothOnOffSwitch();
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
  }

  @Override
  protected void onPause() {
    super.onPause();
  }
}
