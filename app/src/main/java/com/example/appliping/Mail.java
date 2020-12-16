package com.example.appliping;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Mail extends AppCompatActivity {
    EditText E_Text_EmailSender;
    EditText E_Text_PasswordSender;
    EditText E_Text_receiverMail;
    Button B_SendMail;
    private Button B_ChangePage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mail);

        //Definition variables send mail
        E_Text_EmailSender=(EditText)findViewById(R.id.myEmail);
        E_Text_PasswordSender=(EditText)findViewById(R.id.myPassword);
        E_Text_receiverMail=(EditText)findViewById(R.id.receiverEmail);

        //Definition des boutons
        B_SendMail=(Button) findViewById(R.id.SendEmail);
        B_SendMail.setOnClickListener(EcouteurButton);
        B_ChangePage=(Button) findViewById(R.id.Retour);
        B_ChangePage.setOnClickListener(EcouteurBouton2);
    }

    public View.OnClickListener EcouteurButton=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            sendEmail(E_Text_EmailSender.getText().toString(),E_Text_PasswordSender.getText().toString(),E_Text_receiverMail.getText().toString());
        }
    };

    //Retour a la page precedente
    public View.OnClickListener EcouteurBouton2=new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent MainActivity = new Intent(Mail.this, MainActivity.class);
            startActivity(MainActivity);
        }
    };

    //function send mail
    private void sendEmail(final String Sender,final String Password,final String Receiver)
    {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    GMailSender sender = new GMailSender(Sender,Password);
                    sender.sendMail("Relevé", "<b>"+"attente"+"</b>", Sender, Receiver);
                    makeAlert();

                } catch (Exception e) {
                    Toast.makeText(Mail.this,e.getMessage(),Toast.LENGTH_LONG).show();
                }
            }

        }).start();
    }

    //alert the user that the mail is sender
    private void makeAlert(){
        this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(Mail.this, "Mail Sent", Toast.LENGTH_SHORT).show();
            }
        });
    }
}