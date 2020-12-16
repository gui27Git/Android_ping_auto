/*
Last modification:16/12/2020 11:30
Version 1.1
By Guillaume Levasseur and Matisse Dimier
Android application for measuring internet connectivity in GSM data or wifi.
It must be able to retrieve and write internet and GPS data to a file.
for use of the application ,read the readme on Github.
*/
package com.example.appliping;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.InetAddresses;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.ping.PingResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    // init components variables
    private EditText E_Text_Ip = null;
    private EditText E_Text_TimeOut = null;
    private EditText E_Text_Delay = null;
    private Button B_Start = null;
    private Button B_Stop = null;
    private Button B_Mail = null;
    private EditText EditText_list_Acquisition=null;
    // init Global variable
    private FileOutputStream Fos_filePing = null;
    //init variables timer
    private Timer T_timer = null;
    private TimerTask Tt_ping = null;
    private Handler H_ping = new Handler();
    //localisation
    private LocationManager LM_locationManager=null;
    private LocationListener LL_listenerGPS=null;
    private String St_coordinate=null;
    private String St_Resultat_IHM=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);  //Portrait orientation only
        setContentView(R.layout.activity_main);

        //Request at the user for permission to access GPS and storage
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 123);

        LM_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);  //Access localisation system

        //definition variables components
        E_Text_Ip = (EditText) findViewById(R.id.EditAdresseIP);
        E_Text_TimeOut = (EditText) findViewById(R.id.editTimeOut);
        E_Text_Delay = (EditText) findViewById(R.id.editDelay);
        EditText_list_Acquisition=(EditText) findViewById(R.id.EditText_resultAcqui);
        B_Start = (Button) findViewById(R.id.buttonStart);
        B_Stop = (Button) findViewById(R.id.buttonStop);
        B_Mail = (Button) findViewById(R.id.changePage);

        //Listener on button
        B_Start.setOnClickListener(ListenerButton);
        B_Stop.setOnClickListener(ListenerButton);

        B_Mail.setOnClickListener(ListenerButton2);



    }

    //Function Listener for the event
    public View.OnClickListener ListenerButton = new View.OnClickListener() {
        //Event click
        @RequiresApi(api = Build.VERSION_CODES.Q)
        @Override
        public void onClick(View v) {
            if (TextUtils.isEmpty(E_Text_Ip.getText().toString()) == true) { // if E_Text_ip is empty
                E_Text_Ip.setError("IP n'est pas saisi");
                E_Text_Ip.requestFocus();
            }else if(InetAddresses.isNumericAddress(E_Text_Ip.getText().toString())==false){  //if E_text_IP isn't a numeric address
                E_Text_Ip.setError("L'adresse IP est mal saisi");
                E_Text_Ip.requestFocus();
            }
            else if (TextUtils.isEmpty(E_Text_TimeOut.getText().toString()) == true) { //if E_Text_TimeOut is empty
                E_Text_TimeOut.setError("TimeOut n'est pas saisi");
                E_Text_TimeOut.requestFocus();
            } else if(TextUtils.isEmpty(E_Text_Delay.getText().toString())==true){  //if E_Text_Delay is empty
                E_Text_Delay.setError("Le delay n'est pas saisi");
                E_Text_Delay.requestFocus();
            }
            else if(Integer.parseInt(E_Text_Delay.getText().toString())==0){
                E_Text_Delay.setError("le delay ne peut pas avoir une valeur égal a 0");
                E_Text_Delay.requestFocus();
            }
            else if(LM_locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)==false)  //if GPS isn't active
                Toast.makeText(MainActivity.this,"GPS is disabled",Toast.LENGTH_LONG).show();
            else { //else listen state of button
                if (B_Stop.isPressed() == true) {    //Function StopAcquisition
                    StopAcquisition();
                } else if (B_Start.isPressed() == true) {    //Function StartAcquisition
                    StartAcquisition();
                }
            }
        }
    };

    public View.OnClickListener ListenerButton2=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent Mail = new Intent(MainActivity.this, Mail.class) ;
            startActivity(Mail);
        }
    };

    //  Function StartAcquisition
    public void StartAcquisition() {
        E_Text_Ip.setEnabled(false);
        E_Text_TimeOut.setEnabled(false);
        E_Text_Delay.setEnabled(false);
        B_Start.setEnabled(false);
        B_Stop.setEnabled(true);
        B_Mail.setEnabled(false);


        Date D_dateNow = new Date();     //init  format timestamp
        SimpleDateFormat Sdf_Format_date = new SimpleDateFormat("YYYY_MM_dd_HH_mm_ss");
        String S_Format_date_Now = Sdf_Format_date.format(D_dateNow);
        initFile(S_Format_date_Now);

        Get_coordinate();        //Function recuperate data GPS

        T_timer = new Timer();            //init timer
        initializeTimerTask();          //init timerTask
        T_timer.schedule(Tt_ping, 3000, Integer.parseInt(E_Text_Delay.getText().toString()));
    }

    //  Function StopAcquisition
    public void StopAcquisition() {
        if (T_timer != null) {   //if timer not null
            T_timer.cancel();    //cancel timer
            T_timer = null;      //timer reinitialised
        }
        if (Tt_ping != null) {   //if timerTask not null
            Tt_ping.cancel();    //cancel timerTask
            Tt_ping = null;      //timerTask reinitialised
        }
        LM_locationManager.removeUpdates(LL_listenerGPS); //erase update GPS

        //Reinitialisation
        E_Text_Ip.setEnabled(true);
        E_Text_TimeOut.setEnabled(true);
        E_Text_Delay.setEnabled(true);
        B_Start.setEnabled(true);
        B_Stop.setEnabled(false);
        EditText_list_Acquisition.getText().clear();
        B_Mail.setEnabled(true);

        try {
            Fos_filePing.close(); //close  file
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    // Function initializeTimerTask
    public void initializeTimerTask() {
        //Define  timerTask
        Tt_ping = new TimerTask() {
            int compteurPing = 0; //counter number ping

            @Override
            public void run() {
                H_ping.post(new Runnable() {
                    public void run() {
                        try {
                            compteurPing++; //counter increment
                            Log.d("Acquisition", "begin of ping N°" + compteurPing);     //write on the console
                            PingResult pingResult = Ping.onAddress(E_Text_Ip.getText().toString()).setTimeOutMillis(Integer.parseInt(E_Text_TimeOut.getText().toString())).doPing(); //ping on IP address and the timeOut
                            if (pingResult.isReachable() == true) {     //if ping success
                                Log.d("Acquisition", "success ping N°"+compteurPing);  //write result ping on the console
                            } else {  //if ping failed
                                Log.d("Acquisition", "failed ping N°"+compteurPing);     //write result ping on the console
                            }
                            Write_acquisition_IHM(compteurPing,pingResult.getTimeTaken(),St_coordinate);
                            Write_acquisition(pingResult.getTimeTaken(),St_coordinate,pingResult.isReachable());      //Call Function Write_acquisition
                        } catch (UnknownHostException e) {
                            Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        };
    }

    //Function returns if the SD card is writable
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) == true) {
            return true;
        }
        return false;
    }

    //function returns if the SD card is readable
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) == true ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) == true) {
            return true;
        }
        return false;
    }

    //Function init file
    public void initFile(String S_timestamp) {
        try {
            String filename = S_timestamp + "_test.txt";        //name of the file
            File file = new File(getExternalFilesDir(null), filename);
            Fos_filePing = new FileOutputStream(file, true);     //create a new file
            Fos_filePing.write(new String("date;temps_de_reponse;gps\n").getBytes());    //Header of the file
        } catch (FileNotFoundException e) {
            Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
        }
    }

    //function write data into interface
    private void Write_acquisition_IHM(Integer In_nmPing,Float F_TimePing,String St_coordinateGPS){
        Date D_dateNow = new Date();          //Date of the ping
        SimpleDateFormat SDF_formatDate = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss");
        String date = SDF_formatDate.format(D_dateNow);
        String LastPing=In_nmPing+";"+date+";"+F_TimePing+"ms;"+St_coordinateGPS+"\n";
        St_Resultat_IHM=LastPing+St_Resultat_IHM;
        EditText_list_Acquisition.setText(St_Resultat_IHM);
    }

    //Function write data on the file
    public void Write_acquisition(Float F_TimePing,String St_coordinateGPS,Boolean B_statePing) {
        Date D_dateNow = new Date();          //Date of the ping
        SimpleDateFormat SDF_formatDate = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss");
        String date = SDF_formatDate.format(D_dateNow);
        try {
            Log.d("Acquisition", "Write file ...");
            if (isExternalStorageWritable() == true) {
                Fos_filePing.write(new String(date + ";" + F_TimePing + "ms" + ";" + St_coordinateGPS + ";" + B_statePing + "\n").getBytes());
            } else if(isExternalStorageWritable()==false) { //not possible to open the file
                throw new IOException("Your SD card is not available for writing!");
            }
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
            StopAcquisition();
        }
    }
    
    //function who return the GPS data
    public void Get_coordinate() {
        Log.d("Acquisition", "Recuperation GPS data");
        try {
            try {
                LM_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, LL_listenerGPS= new LocationListener()
                {
                    @Override
                    public void onLocationChanged(@NonNull Location Lo_location) {
                         St_coordinate = String.format( Lo_location.getLatitude()+","+ Lo_location.getLongitude());
                    }
                    @Override
                    public void onProviderDisabled(@NonNull String S_provider) {
                        StopAcquisition();
                    }
                });
            } catch (Exception e) {
                Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_LONG).show();
            }
        }
        catch (SecurityException ex){
            Toast.makeText(MainActivity.this,ex.getMessage(),Toast.LENGTH_LONG).show();
        }
    }
}