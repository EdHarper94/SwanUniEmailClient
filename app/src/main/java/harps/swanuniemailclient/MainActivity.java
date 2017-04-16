package harps.swanuniemailclient;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        // SEND EMAIL
        Email email = new Email (EmailUser.getEmailAddress(), "FIRST EMAIL", "This is the first email test from prototype 1");
        SendEmail sm = new SendEmail(this, email);
        sm.execute();
        System.out.println("EMAIL SENT 2");

       */

        /*
        Intent inboxIntent = new Intent("com.egwh.swanuniemailclient.Inbox");
        startActivity(inboxIntent);
        */

        Intent inboxIntent = new Intent("com.egwh.swanuniemailclient.SendEmail");
        startActivity(inboxIntent);


    }
}

