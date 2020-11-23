package com.example.appliping;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.ping.PingResult;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    //initialisation des variables global
        //initialisation des variables components
    private EditText E_Text_Ip=null;
    private EditText E_Text_TimeOut=null;
    private Button B_Start=null;
    private Button B_Stop=null;
        //initialisation des variables pour le timer
    private Timer T_timer=null;
    private TimerTask Tt_ping=null;
    Handler H_ping = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //definition des variables aux components de l'apllication
        setContentView(R.layout.activity_main);
        E_Text_Ip = (EditText) findViewById(R.id.EditAdresseIP);
        E_Text_TimeOut = (EditText) findViewById(R.id.editTimeOut);
        B_Start = (Button) findViewById(R.id.buttonStart);
        B_Stop=(Button)findViewById(R.id.buttonStop);
        //Ecouteur sur les button pour recuperate les event
        B_Start.setOnClickListener(EcouteurButton);
        B_Stop.setOnClickListener(EcouteurButton);
    }
    /*
    Fonction qui écoute l'état du button
     */
    public View.OnClickListener EcouteurButton=new View.OnClickListener() {
        //Event click sur le boutton
        @Override
        public void onClick(View v) {

            if(TextUtils.isEmpty(E_Text_Ip.getText().toString())){ //Si le champ de text de l'adresse IP est vide
                E_Text_Ip.setHintTextColor(Color.RED);  //afficher un placeholder rouge
                E_Text_Ip.setHint("IP n'est pas saisi");
            }else if (TextUtils.isEmpty(E_Text_TimeOut.getText().toString())){ //si le champ de text du timeOut est vide
                E_Text_TimeOut.setHintTextColor(Color.RED); //Afficher un placeholder rouge
                E_Text_TimeOut.setHint("TimeOut n'est pas saisi");
            }
            else { //Sinon on regarde l'état des bouton
                if (B_Stop.isPressed()== true) {    //si le bouton stop est pressé on lance la fonction stopTimer
                    StopTimer();
                } else if (B_Start.isPressed()){    //si le bouton start est pressé on lance la fonction startTimer
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
    public void StopTimer(){
        if(T_timer!=null){    //si le timer n'est pas null
            T_timer.cancel(); //annuler le timer
            T_timer=null;     //timer renitialised
        }
        if (Tt_ping!=null) {     //si le timerTask
            Tt_ping.cancel();    //annuler timerTask
            Tt_ping=null;        //timerTask reinitialised
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
        public void initializeTimerTask(){
        //Definition du timerTask
        Tt_ping=new TimerTask() {
            int compteurPing=0; //compteur de ping
            @Override
            public void run() {
                H_ping.post(new Runnable() {
                    public void run() {
                        try {
                            compteurPing++; //incrément du compteur
                            Log.i("début", "début du ping= "+compteurPing);     //écriture dans la console
                            PingResult pingResult = Ping.onAddress(E_Text_Ip.getText().toString()).setTimeOutMillis(Integer.parseInt(E_Text_TimeOut.getText().toString())).doPing(); //ping effécué a l'adresse IP et avec le timeOut
                            if (pingResult.isReachable()) {     //Si le ping est effctué
                                Log.i("succès", "ping réussi ");
                                Toast toast = Toast.makeText(MainActivity.this,"temps du ping "+pingResult.getTimeTaken()+"ms",Toast.LENGTH_SHORT); //affcicher le résultat dans un toast
                                toast.show();
                            }
                            else {  //si le ping n'a pas abouti
                                Log.i("echec du ping ","le problème est de "+ pingResult.toString());
                                Toast toast = Toast.makeText(MainActivity.this, "connxion échoué , le timeOut est dépassé "+pingResult.getError(),Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }
                    };
                });
            }
        };


    }
}