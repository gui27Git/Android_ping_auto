package com.example.appliping;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.ping.PingResult;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    //initialisation des variables global
    //initialisation des variables components
    private EditText E_Text_Ip = null;
    private EditText E_Text_TimeOut = null;
    private EditText E_Text_Delay = null;
    private Button B_Start = null;
    private Button B_Stop = null;
    private Spinner S_listeAquisi=null;
    private Date D_dateNow = null;
    ArrayList<String>AL_listeAqui=new ArrayList<String>();
    private FileOutputStream Fos_filePing = null;
    //initialisation des variables pour le timer
    private Timer T_timer = null;
    private TimerTask Tt_ping = null;
    Handler H_ping = new Handler();

    private LocationManager LM_locationManager=null;
    private String St_coordonees=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //definition des variables aux components de l'apllication
        setContentView(R.layout.activity_main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.MANAGE_EXTERNAL_STORAGE}, 123);

        LM_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        E_Text_Ip = (EditText) findViewById(R.id.EditAdresseIP);
        E_Text_TimeOut = (EditText) findViewById(R.id.editTimeOut);
        E_Text_Delay = (EditText) findViewById(R.id.editDelay);
        B_Start = (Button) findViewById(R.id.buttonStart);
        B_Stop = (Button) findViewById(R.id.buttonStop);
        S_listeAquisi=(Spinner)findViewById(R.id.spinner_resultAcqui);

        //Ecouteur sur les button pour recuperate les event
        B_Start.setOnClickListener(EcouteurButton);
        B_Stop.setOnClickListener(EcouteurButton);


    }

    /*
    Fonction qui écoute l'état du button
     */
    public View.OnClickListener EcouteurButton = new View.OnClickListener() {
        //Event click sur le boutton
        @Override
        public void onClick(View v) {

            if (TextUtils.isEmpty(E_Text_Ip.getText().toString()) == true) { //Si le champ de text de l'adresse IP est vide
                E_Text_Ip.setHintTextColor(Color.RED);  //afficher un placeholder rouge
                E_Text_Ip.setHint("IP n'est pas saisi");
            } else if (TextUtils.isEmpty(E_Text_TimeOut.getText().toString()) == true) { //si le champ de text du timeOut est vide
                E_Text_TimeOut.setHintTextColor(Color.RED); //Afficher un placeholder rouge
                E_Text_TimeOut.setHint("TimeOut n'est pas saisi");
            } else { //Sinon on regarde l'état des bouton
                if (B_Stop.isPressed() == true) {    //si le bouton stop est pressé on lance la fonction StopAcquisition
                    StopAcquisition();
                } else if (B_Start.isPressed() == true) {    //si le bouton start est pressé on lance la fonction StartAcquisition
                    StartAcquisition();
                }
            }
        }

    };

    //  Fonction StartAcquisition
    public void StartAcquisition() {
        E_Text_Ip.setEnabled(false);
        E_Text_TimeOut.setEnabled(false);
        E_Text_Delay.setEnabled(false);
        B_Start.setEnabled(false);
        B_Stop.setEnabled(true);

        D_dateNow = new Date();     //initialisation du format d'horodatage
        SimpleDateFormat Sdf_Formatdate = new SimpleDateFormat("YYYY_MM_dd_HH_mm_ss");
        String S_FormatdateNow = Sdf_Formatdate.format(D_dateNow);
        initFile(S_FormatdateNow);

        GetCordonnees();        //fonction qui récupere les données GPS

        T_timer = new Timer();            //initialisation du timer
        initializeTimerTask();          //initialisation du timerTask
        T_timer.schedule(Tt_ping, 1000, Integer.parseInt(E_Text_Delay.getText().toString()));


    }

    //  Fonction StopAcquisition
    public void StopAcquisition() {
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
        E_Text_Delay.setEnabled(true);
        E_Text_Ip.getText().clear();
        E_Text_TimeOut.getText().clear();
        E_Text_Delay.getText().clear();
        B_Start.setEnabled(true);
        B_Stop.setEnabled(false);

        try {
            Fos_filePing.close(); //Fermeture du fichier
        } catch (IOException e) {
            e.printStackTrace();
        }

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
                            if (pingResult.isReachable() == true) {     //Si le ping est effctué
                               // Toast.makeText(MainActivity.this, "ping réussi ", Toast.LENGTH_SHORT).show();
                                Log.d("ping", "Reussite du ping N°"+compteurPing);
                            } else {  //si le ping n'a pas abouti
                                //Toast.makeText(MainActivity.this, "connxion échoué , le timeOut est dépassé " + pingResult.getError(), Toast.LENGTH_SHORT).show();
                                Log.d("ping", "Echec du ping N°"+compteurPing);
                            }
                            Ecriture_acquisition(pingResult.getTimeTaken());
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    }

                });
            }
        };
    }

    //Fonction qui retourne si la card SD est écrivable
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) == true) {
            return true;
        }
        return false;
    }

    //fonction qui retourne si la carte SD est lisable
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) == true ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state) == true) {
            return true;
        }
        return false;
    }

    //Fonction d'initialisation du fichier
    public void initFile(String S_hordatage) {
        try {
            String filename = S_hordatage + "_test.txt";
            File file = new File(getExternalFilesDir(null), filename);
            Fos_filePing = new FileOutputStream(file, true);
            Fos_filePing.write(new String("date;temps_de_reponse;gps\n").getBytes());    //Ecriture des données

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Fonction qui écrit les donnée dans le fichier
    public void Ecriture_acquisition(Float tempsPing) {
        Date now = new Date();          //Date
        SimpleDateFormat formatDate = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss");
        String date = formatDate.format(now);

        AL_listeAqui.add(date+";"+tempsPing+";"+St_coordonees+"\n");
        S_listeAquisi.setAdapter(new ArrayAdapter<String>(MainActivity.this,R.layout.support_simple_spinner_dropdown_item,AL_listeAqui));
        
        if (isExternalStorageWritable() == true) {
            try {
                Log.d("Aquisition", "Ecriture CSV en cours...");
                
                Fos_filePing.write(new String(date + ";" + tempsPing + "ms"+";"+St_coordonees+"\n").getBytes());
            } catch (IOException e) {
                System.out.println(e);
            }
        } else { //impossible d'ouvrir le fichier
            CharSequence text = "Votre carte SD n'est pas disponible à l'écriture !";
            AlertDialog.Builder ALertDialog_alertError_fichier = new AlertDialog.Builder(MainActivity.this);
            ALertDialog_alertError_fichier.setTitle("erreur Fichier");
            ALertDialog_alertError_fichier.setMessage(text);
            ALertDialog_alertError_fichier.show();
            StopAcquisition();
        }
    }
    
        //fonction qui recupere les données GPS actuel
    public void GetCordonnees(){
        Log.d("Aquisition", "Récupération des données GPS");
       LocationProvider LP_fournisseur = LM_locationManager.getProvider("gps");
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

         Location L_localisation = LM_locationManager.getLastKnownLocation(LP_fournisseur.getName());
             St_coordonees = String.format("-%f -%f\n", L_localisation.getAltitude(), L_localisation.getLongitude());

        }
    }
}





