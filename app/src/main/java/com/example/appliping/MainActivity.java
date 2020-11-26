package com.example.appliping;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.graphics.Color;
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
    //initialisation des variables global
    //initialisation des variables components
    private EditText E_Text_Ip = null;
    private EditText E_Text_TimeOut = null;
    private FileOutputStream Fos_filePing=null;
    private Button B_Start = null;
    private Button B_Stop = null;
    //initialisation des variables pour le timer
    private Timer T_timer = null;
    private TimerTask Tt_ping = null;
    Handler H_ping = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //definition des variables aux components de l'apllication
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},123);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},123);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.MANAGE_EXTERNAL_STORAGE},123);

        E_Text_Ip = (EditText) findViewById(R.id.EditAdresseIP);
        E_Text_TimeOut = (EditText) findViewById(R.id.editTimeOut);
        B_Start = (Button) findViewById(R.id.buttonStart);
        B_Stop = (Button) findViewById(R.id.buttonStop);
        //Ecouteur sur les button pour recuperate les event
        B_Start.setOnClickListener(EcouteurButton);
        B_Stop.setOnClickListener(EcouteurButton);
        initFile();
    }

    /*
    Fonction qui écoute l'état du button
     */
    public View.OnClickListener EcouteurButton = new View.OnClickListener() {
        //Event click sur le boutton
        @Override
        public void onClick(View v) {

            if (TextUtils.isEmpty(E_Text_Ip.getText().toString())) { //Si le champ de text de l'adresse IP est vide
                E_Text_Ip.setHintTextColor(Color.RED);  //afficher un placeholder rouge
                E_Text_Ip.setHint("IP n'est pas saisi");
            } else if (TextUtils.isEmpty(E_Text_TimeOut.getText().toString())) { //si le champ de text du timeOut est vide
                E_Text_TimeOut.setHintTextColor(Color.RED); //Afficher un placeholder rouge
                E_Text_TimeOut.setHint("TimeOut n'est pas saisi");
            } else { //Sinon on regarde l'état des bouton
                if (B_Stop.isPressed()) {    //si le bouton stop est pressé on lance la fonction stopTimer
                    StopTimer();
                } else if (B_Start.isPressed()) {    //si le bouton start est pressé on lance la fonction startTimer
                    startTimer();
                }
            }
        }

    };

    //  Fonction StartTimer
    public void startTimer() {
        E_Text_Ip.setEnabled(false);
        E_Text_TimeOut.setEnabled(false);
        B_Start.setEnabled(false);
        B_Stop.setEnabled(true);

        T_timer = new Timer();            //initialisation du timer
        initializeTimerTask();          //initialisation du timerTask
        T_timer.schedule(Tt_ping, 1000, 5000);


    }

    //  Fonction StopTimer
    public void StopTimer() {
        if (T_timer != null) {    //si le timer n'est pas null
            T_timer.cancel(); //annuler le timer
            T_timer = null;     //timer renitialised
        }
        if (Tt_ping != null) {     //si le timerTask
            Tt_ping.cancel();    //annuler timerTask
            Tt_ping = null;        //timerTask reinitialised
        }
        //Reinitialisation de la page
        E_Text_Ip.setEnabled(true);
        E_Text_TimeOut.setEnabled(true);
        E_Text_Ip.getText().clear();
        E_Text_TimeOut.getText().clear();
        B_Start.setEnabled(true);
        B_Stop.setEnabled(false);

    }

    // Fonction initilazeTimerTask
    public void initializeTimerTask() {
        //Definition du timerTask
        Tt_ping = new TimerTask() {
            int compteurPing = 0; //compteur de ping

            @Override
            public void run() {
                H_ping.post(new Runnable() {
                    public void run() {
                        try {
                            compteurPing++; //incrément du compteur
                            Log.i("début", "début du ping= " + compteurPing);     //écriture dans la console
                            PingResult pingResult = Ping.onAddress(E_Text_Ip.getText().toString()).setTimeOutMillis(Integer.parseInt(E_Text_TimeOut.getText().toString())).doPing(); //ping effécué a l'adresse IP et avec le timeOut
                            if (pingResult.isReachable()) {     //Si le ping est effctué
                                Toast.makeText(MainActivity.this, "ping réussi ", Toast.LENGTH_SHORT).show();
                            } else {  //si le ping n'a pas abouti
                                Toast toast = Toast.makeText(MainActivity.this, "connxion échoué , le timeOut est dépassé " + pingResult.getError(), Toast.LENGTH_SHORT);
                                toast.show();
                            }
                            EcritureCSV(pingResult.getTimeTaken());
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }

                });
            }
        };
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
    //Fonction d'initialisation du fichier
    public void initFile(){
        try {
            String filename = "test.txt";
            File file = new File(getExternalFilesDir(null), filename);
            Fos_filePing=new FileOutputStream(file,true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Fonction qui écrit les donnée dans le fichier
    public void EcritureCSV(Float tempsPing) {
            if (isExternalStorageWritable()) {
                try {
                    Date now = new Date();          //Date
                    SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss");
                    String date = formatDate.format(now);
                    Fos_filePing.write(new String("date;temps_de_reponse;gps\n").getBytes());    //Ecriture des données
                    Fos_filePing.write(new String(date + ";" + tempsPing + "ms\n").getBytes());
                    Fos_filePing.close(); //Fermeture du fichier
                } catch (FileNotFoundException e) {
                    System.out.println(e);
                } catch (IOException e) {
                    System.out.println(e);
                }
            } else { //impossible d'ouvrir le fichier 
                CharSequence text = "Votre carte SD n'est pas disponible à l'écriture !";
                Toast.makeText(MainActivity.this,text,Toast.LENGTH_LONG).show();


            }
        }
    }





