package de.hdmstuttgart.blueiot;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {
    private BleDeviceListAdapter bleDeviceListAdapter;

    private Handler handler;

    private boolean isScanning;
    private boolean isBluetoothSupported;
    private boolean isBleSupported;

    private BluetoothAdapter bluetoothAdapter;

    private static final int CONTEXT_MENU_INSPECT = 0;
    private static final int CONTEXT_MENU_BALANCE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setup for the ListView and its Adapter
        ListView listView = (ListView) this.findViewById(R.id.listView);
        this.bleDeviceListAdapter = new BleDeviceListAdapter(this);
        listView.setAdapter(this.bleDeviceListAdapter);

        //Used for asynchronous tasks
        this.handler = new Handler();

        //Bluetooth Components
        BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        //Check if Bluetooth is supported on the device
        if (this.bluetoothAdapter != null) {
            this.isBluetoothSupported = true;

            //Check if BLE is supported on the device
            if (this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                this.isBleSupported = true;
            }
        }

        //Handle clicks being made onto the ListView's Items
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //If still scanning for BLE-Devices, stop scanning immediately before starting the new Activity
                if (isScanning) {
                    scanLeDevice(false);
                }

                BluetoothDevice device = bleDeviceListAdapter.getDevice(position);
                if (device != null) {
                    if ((device.getName() != null && device.getName().contains("iBeacon")) || device.getAddress().equals("00:07:80:7F:A6:E0")) {
                        //Pass over the BluetoothDevice to the new Activity using the Intent
                        Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                        intent.putExtra("device", device);

                        //Start DetailActivity
                        startActivity(intent);
                    }
                }
                else {
                    //Unable to find the BluetoothDevice in the ListAdapter
                    Toast.makeText(MainActivity.this, "Unable to get the selected Bluetooth Device. Try scanning again...", Toast.LENGTH_LONG).show();
                    bleDeviceListAdapter.clear();
                }
            }
        });

        this.registerForContextMenu(listView);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_scan:
                //Check if Bluetooth is supported on the device
                if (this.isBluetoothSupported) {
                    //Check if BLE is supported on the device
                    if (this.isBleSupported) {
                        //Check if Bluetooth is currently enabled
                        if (this.bluetoothAdapter.isEnabled()) {
                            if (this.isScanning) {
                                scanLeDevice(false);
                            }
                            else {
                                scanLeDevice(true);
                            }
                        }
                        else {
                            //Show a Dialog, allowing the User to turn Bluetooth ON
                            //Start scanning for BLE-Devices if the User does turn it on (see onActivityResult)
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            this.startActivityForResult(enableBtIntent, 1337);
                        }
                    }
                    else {
                        Toast.makeText(this, "Can't Scan: BLE not supported.", Toast.LENGTH_LONG).show();
                    }
                }
                else {
                    Toast.makeText(this, "Can't Scan: Bluetooth not supported.", Toast.LENGTH_LONG).show();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //Stop scanning and clear any data from the Adapter
        scanLeDevice(false);
        this.bleDeviceListAdapter.clear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1337) {
            if (resultCode == -1) {
                if (!this.isScanning) {
                    scanLeDevice(true);
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        //Get Reference to the Item that was selected
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
        BluetoothDevice device = this.bleDeviceListAdapter.getDevice(acmi.position);

        if (device.getName() != null) {
            menu.setHeaderTitle(device.getName());
        }
        else {
            if (device.getAddress() != null) {
                menu.setHeaderTitle(device.getAddress());
            }
            else {
                menu.setHeaderTitle("Bluetooth Device");
            }
        }

        //Add Context Menu Items
        menu.add(Menu.NONE, CONTEXT_MENU_INSPECT, Menu.NONE, "Inspect Device");
        menu.add(Menu.NONE, CONTEXT_MENU_BALANCE, Menu.NONE, "Balance Ball");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case CONTEXT_MENU_INSPECT: {
                if (this.isScanning) {
                    scanLeDevice(false);
                }

                //TODO
                //Start new Activity
                BluetoothDevice device = this.bleDeviceListAdapter.getDevice(acmi.position);
                //Toast.makeText(this, "Clicked ContextMenu for: " + device.getAddress(), Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, InspectDeviceActivity.class);
                intent.putExtra("device", device);

                startActivity(intent);
                return true;
            }
            case CONTEXT_MENU_BALANCE: {
                if (this.isScanning) {
                    scanLeDevice(false);
                }

                BluetoothDevice device = this.bleDeviceListAdapter.getDevice(acmi.position);
                if (device != null) {
                    if ((device.getName() != null && device.getName().contains("iBeacon")) || device.getAddress().equals("00:07:80:7F:A6:E0")) {
                        Intent intent = new Intent(this, DrawActivity.class);
                        intent.putExtra("device", device);
                        this.startActivity(intent);
                    }
                }
                else {
                    //Unable to find the BluetoothDevice in the ListAdapter
                    Toast.makeText(this, "Unable to get the selected Bluetooth Device. Try scanning again...", Toast.LENGTH_LONG).show();
                    this.bleDeviceListAdapter.clear();
                }
                return true;
            }
            default:
                return super.onContextItemSelected(item);
        }
    }

    //Device Scan Callback
    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            //Update the UI with all BLE-Devices that were found
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bleDeviceListAdapter.addDevice(device);
                    bleDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            //Stops scanning after a defined scan period
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    bluetoothAdapter.stopLeScan(leScanCallback);
                }
            }, 5000);

            //Start scanning for BLE-Devices
            isScanning = true;
            bluetoothAdapter.startLeScan(leScanCallback);
        }
        else {
            //Stop scanning immediately
            isScanning = false;
            bluetoothAdapter.stopLeScan(leScanCallback);
        }
    }
}