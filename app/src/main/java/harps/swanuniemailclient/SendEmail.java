package harps.swanuniemailclient;

import android.app.ProgressDialog;
import android.content.Context;

import android.os.AsyncTask;
import android.widget.Toast;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Class to send email via University IMAP server
 *
 * @author Edward Harper
 * @date 29/03/17
 */

public class SendEmail extends AsyncTask<Void, Void, Void>{

    private Context context;
    private Session session;

    private Email email;

    private ProgressDialog pd;

    // Init IMAP Settings
    private ImapSettings imapSettings = new ImapSettings();
    Properties props;

    public SendEmail(Context context, Email email){
        this.context = context;
        this.email = email;
    }

    @Override
    protected void onPreExecute(){
        super.onPreExecute();
        pd = new ProgressDialog(context);
        pd.setMessage("Sending Email...");
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.show();
    }

    public Properties setServerProperties(){
        props = new Properties();
        props.put("mail.smtp.from", EmailUser.getEmailAddress());

        // Server settings
        props.put("mail.smtp.host", imapSettings.getServerAddress());
        props.put("mail.smtp.port", imapSettings.getOutPort());
        props.put("mail.smtp.auth", "true");

        // SSL/STARTTLS settings
        props.put("mail.smtp.socketFactory.port", imapSettings.getOutPort());
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactor.fallback", "false");
        props.put("mail.smtp.starttls.enable", true);

        // DEBUG
        props.put("mail.smtp.connectiontimeout", "5000");

        return props;
    }

    @Override
    protected Void doInBackground(Void...params){

        // Add property settings
        props = setServerProperties();

        session = Session.getDefaultInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EmailUser.getEmailAddress(), EmailUser.getPassword());
            }
        });


        System.out.println("PASSWORD AUTHENTICATED");
        try{
            // Create new message object and add details
            MimeMessage mm = new MimeMessage(session);
            mm.setFrom(new InternetAddress((EmailUser.getEmailAddress())));
            mm.addRecipient(Message.RecipientType.TO, new InternetAddress("e.g.harper@hotmail.co.uk"));
            mm.setSubject(email.getSubject(), "UTF-8");
            mm.setText(email.getText());

            Transport.send(mm);
            System.out.println("EMAIL SENT");
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result){
        super.onPostExecute(result);
        pd.dismiss();
        Toast.makeText(context, "Message sent!", Toast.LENGTH_LONG).show();
    }
}

