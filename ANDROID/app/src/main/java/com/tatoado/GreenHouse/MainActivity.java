package com.tatoado.GreenHouse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.os.Vibrator;

import com.tatoado.GreenHouse.R;
//Vasdasd

public class MainActivity extends Activity implements SensorEventListener, CompoundButton.OnCheckedChangeListener {

    private boolean estadoLuz;
    private boolean sensorCorriendo = false;
    private final static float ACC = 30;
    private SensorManager sensor;
    Button btnOn, btnOff,regarOn,ventilarOn,ventilarOff,btnDatos,btnDatosHum;
    TextView txtString;
    TextView txtStringLength;
    TextView sensorView0;
    TextView tempDeseada;
    TextView humedadDeseada;
    TextView txtSendorLDR;
    TextView textDatoHumedad;
    TextView textDatoTemperatura;
    TextView ingresoTemp;
    TextView ingresoHum;
    ToggleButton toggleAutomatic;
    Handler bluetoothIn;

    final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static String address = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        sensor = (SensorManager) getSystemService(SENSOR_SERVICE);
        boolean b = sensor.registerListener(this, sensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);

        // SI NO SE PUDO REGISTRAR EL SENSOR CERERAMOS
        if( b == false ) {
            this.onDestroy();
        }
        boolean c = sensor.registerListener(this, sensor.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_UI);
        // SI NO SE PUDO REGISTRAR EL SENSOR CERERAMOS
        if( c == false ) {
            this.onDestroy();
        }
        //Componentes del activityMain
        textDatoHumedad = (TextView) findViewById(R.id.textDatoHumedadLayOut);
        textDatoTemperatura = (TextView) findViewById(R.id.textDatoTemperaturaLayOut);
        btnOn = (Button) findViewById(R.id.btnOn);
        btnOff = (Button) findViewById(R.id.btnOff);
        btnDatos = (Button) findViewById(R.id.btnDatos);
        btnDatosHum = (Button) findViewById(R.id.btnDatosHum);
        regarOn = (Button) findViewById(R.id.regarOn);
        ventilarOn = (Button) findViewById(R.id.ventilarOn);
        ventilarOff = (Button) findViewById(R.id.ventilarOff);
        toggleAutomatic = (ToggleButton) findViewById(R.id.toggleAutomatic);
        txtString = (TextView) findViewById(R.id.txtString);
        txtStringLength = (TextView) findViewById(R.id.testView1);
        ingresoTemp = (TextView) findViewById(R.id.ingresoTemp);
        ingresoHum = (TextView) findViewById(R.id.ingresoHum);
        tempDeseada = (TextView) findViewById(R.id.tempDeseada);
        humedadDeseada = (TextView) findViewById(R.id.humedadDeseada);
        //sensorView0 = (TextView) findViewById(R.id.sensorView0);
        txtSendorLDR = (TextView) findViewById(R.id.tv_sendorldr);

        bluetoothIn = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {

                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);

                    int endOfLineIndex = recDataString.indexOf("~");                   //donde termina nuestra linea de comunicacion
                    if (endOfLineIndex > 0) {                                           // nos aseguramos que encontramos el fin de linea
                        String dataInPrint = recDataString.substring(0, endOfLineIndex);    //  tomamos el string
                        txtString.setText(dataInPrint);
                        int dataLength = dataInPrint.length();							//tomamos el tamanio del string recibido
                        //txtStringLength.setText("Tamaño del String = " + String.valueOf(dataLength));

                        if (recDataString.charAt(0) == '#')								//detectamos comienzo de la comunicacion
                        {
                            String cadenaParseada[] = recDataString.toString().split(";");
                            //tomamos los datos
                            String sensor0 = cadenaParseada[0].toString();
                            String sensor1 = cadenaParseada[1].toString();
                            String sensor2 = cadenaParseada[2].toString();
                            String sensor3 = cadenaParseada[3].toString();
                            String sensor4 = cadenaParseada[4].toString();

                            if ( Float.valueOf(sensor1) == 0.00){
                                estadoLuz = false;
                            }else{
                                estadoLuz = true;
                            }

                            if ( Float.valueOf(sensor4) > Float.parseFloat(humedadDeseada.getText().toString())){
                                textDatoHumedad.setText("Seco");
                                
                            }else {
                                textDatoHumedad.setText("Humedo");
                            }
                            textDatoTemperatura.setText(sensor3.toString());

                        }
                        //Luego de procesar los datos limpiamos la variable
                        recDataString.delete(0, recDataString.length());
                        dataInPrint = " ";
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        btnOff.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("2");    // Send "0" via Bluetooth
                Toast.makeText(getBaseContext(), "Apagar el LED", Toast.LENGTH_SHORT).show();
            }
        });

        btnOn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("1");    // Send "1" via Bluetooth
                Toast.makeText(getBaseContext(), "Encender el LED", Toast.LENGTH_SHORT).show();
            }
        });

        btnDatos.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                tempDeseada.setText(ingresoTemp.getText().toString());
                mConnectedThread.write("t"+ ingresoTemp.getText().toString());    // Send "tTemp" via Bluetooth
                Toast.makeText(getBaseContext(), "Temperatura Configurada", Toast.LENGTH_SHORT).show();
            }
        });

        btnDatosHum.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                humedadDeseada.setText(ingresoHum.getText().toString());
                mConnectedThread.write("h"+ ingresoHum.getText().toString());    // Send "hHumedad" via Bluetooth
                Toast.makeText(getBaseContext(), "Humedad Configurada", Toast.LENGTH_SHORT).show();
            }
        });

        ventilarOn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write("4");
            }
        });

        ventilarOff.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write("5");
            }
        });

        toggleAutomatic.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    mConnectedThread.write("A");
                }else{
                    mConnectedThread.write("M");
                }
            }
        });

        regarOn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mConnectedThread.write("R");
                mConnectedThread.write("X");
            }
        });

    }


    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);

    }

    @Override
    public void onResume() {
        super.onResume();

        //tomamos el MAC adress desde el DeviceListActivity
        Intent intent = getIntent();
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        //creamos el dispositivo bluetoth con el mac adress
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }

        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {

            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        // Mandamos un X en caso de onResume() para saber si la conexion sigue funcionando o no en caso de que no funcione va a tirar finish()
        mConnectedThread.write("x");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {

            btSocket.close();
        } catch (IOException e2) {

        }
    }


    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "El dispositivo no soporta bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    @Override
    synchronized public void onSensorChanged(SensorEvent sensorEvent) {
        int sensorType = sensorEvent.sensor.getType();

        if( sensorCorriendo ){
            return;
        }
        sensorCorriendo = true;


        float[] values = sensorEvent.values;
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            if ((Math.abs(values[0]) > ACC) || (Math.abs(values[1]) > ACC) || (Math.abs(values[2]) > ACC)) {
                Log.i("sensor", "running");
                if (estadoLuz == true) {
                    mConnectedThread.write("9");    // Send "1" via Bluetooth
                 //estadoLuz = false;
                    synchronized (this){
                        try {
                            wait(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }else {
                    mConnectedThread.write("8");    // Send "1" via Bluetooth
                 //   estadoLuz = true;
                    synchronized (this){
                        try {
                            wait(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        float[] values2 = sensorEvent.values;
        if (sensorType == Sensor.TYPE_LIGHT) {
            if( values2[0] < 15 ){

                v.vibrate(1000);
                Toast.makeText(getBaseContext(), "Hay poca luz: " + Float.toString(values2[0]), Toast.LENGTH_SHORT).show();
            }
        }
        sensorCorriendo = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

    }



    // creamos una nueva clase para conectarnos al hilo
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {

                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[256];
            int bytes;


            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);

                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }


        public void write(String input) {
            byte[] msgBuffer = input.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {

                Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }
}

