## SDK 다운로드
[![Platform](https://img.shields.io/badge/platform-android-orange.svg)]()
[![Languages](https://img.shields.io/badge/language-java7-yellow.svg)]()
[![Gradle](https://img.shields.io/badge/gradle-4.4-blue.svg)]()

현재 최신 버전은 `v1.1.13` 입니다  [Download latest version](/app/libs)

## permission 요구사항
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```


## Example Import

repository clone

```
git clone git@github.com:zoyi/ble_sdk_example.git
```

`IntelliJ` or `Android Studio` Import project.</br>
임포트 완료후 실행 시키면 됩니다.

## SDK 활용 방법

### ScannerReceiver interface 설명
- void destory(): 사용하는 리소스 release
- getIsActivating(): scan이 start 되어 있는지
- setIsActivating(): start/stop 시 내부적으로 사용
- startScan(Context context): scan start
- stopScan(Context context): scan stop

----------------------------

### BluetoothReceiver 활용 방법

- 모바일 기기의 Bluetooth on/off state 확인 방법 [DeviceScanActivity](/app/src/main/java/com/zoyi/commutecheck/app/Activity/DeviceScanActivity.java) 코드 참고

```java
  public void registerBluetoothStateChangeReceiver() {
    mBluetoothStateChangeReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
          final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
          switch (state) {
            case BluetoothAdapter.STATE_OFF:
            case BluetoothAdapter.STATE_TURNING_OFF:
            case BluetoothAdapter.STATE_ON:
            case BluetoothAdapter.STATE_TURNING_ON:
              syncBluetoothOnOffSwitch();
              break;
          }
        }
      }
    };
    IntentFilter bleStateChangeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    registerReceiver(mBluetoothStateChangeReceiver, bleStateChangeFilter);
  }
```

> 사용할 경우 unregister receiver를 호출 해주셔야 합니다. [아래 참고](Release receiver, stateChangeReceiver)

- BleReceiver 생성, Callback 등록

```java
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
    } else if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      bleReceiver = new LegacyBleReceiver(this, targetMacs, new ScannerReceiverCallback() {
        @Override
        public void onSuccess(ZoyiSignal value) {
          validateRecord(value);
        }
      });
    }
  }
```

----------------------------

### WifiReceiver 활용 방법

- 모바일 기기의 Wifi on/off state 확인 방법 [DeviceScanActivity](/app/src/main/java/com/zoyi/commutecheck/app/Activity/DeviceScanActivity.java) 코드 참고

```java
  public void registerWifiSTateChangeReceiver() {
    mWifiStateChangeReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
          int WifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
          switch (WifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
            case WifiManager.WIFI_STATE_DISABLED:
              syncWifiOnOffSwitch();
              break;
          }
        }
      }
    };
    IntentFilter wifiStateChangeFilter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
    registerReceiver(mWifiStateChangeReceiver, wifiStateChangeFilter);
  }
```

> 사용할 경우 unregister receiver를 호출 해주셔야 합니다. [아래 참고](Release receiver, stateChangeReceiver)

- WifiReceiver 생성, Callback 등록

```java
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
```

### Callback Rssi Validate

```java
  public void validateRecord(ZoyiSignal record) {
    Log.v(DeviceScanActivity.class.toString(), record.toString());
    addMonitoringTargetMacRssi(record);
    if (isValidateRecord(record)) {
      commuteSuccess(record);
      Log.v(DeviceScanActivity.class.toString(), "commuteSuccess");
    } else {
      Log.d(DeviceScanActivity.class.toString(), "rssi to weak");
    }
  }
```

```java
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
```

### Release receiver, stateChangeReceiver

in Activity

```java
  @Override
  protected void onDestroy() {
    super.onDestroy();

    unregisterReceiver(mBluetoothStateChangeReceiver);
    unregisterReceiver(mWifiStateChangeReceiver);
    if (wifiReceiver != null) {
      wifiReceiver.destory();
    }
    if (bleReceiver != null) {
      bleReceiver.destory();
    }
  }
```
