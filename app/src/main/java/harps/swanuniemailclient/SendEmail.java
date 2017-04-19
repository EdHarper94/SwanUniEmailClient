package harps.swanuniemailclient;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Created by eghar on 17/04/2017.
 */

public class SendEmail extends AsyncTask<Void, Void, String> {

    /**
     * Interface to receive result of send email async task
     */
    public interface SendEmailResponse{
        void sendEmailResult(String result);
    }

    public SendEmailResponse delegate = null;

    private String FINISHED = "Y";

    private Exception exception;
    private Session session;

    private ProgressDialog pd;

    // Init Settings
    private Properties props = new ServerProperties().getSMTPProperties();

    private OutgoingEmail outEmail;
    private Context context;

    private ArrayList<String> recipients = new ArrayList<>();
    private ArrayList<String> ccRecipients = new ArrayList<>();
    private ArrayList<String> bccRecipients = new ArrayList<>();
    private ArrayList<File> attachments = new ArrayList<>();

    private Boolean attachment;

    public SendEmail(Context context, OutgoingEmail outEmail, SendEmailResponse delegate){
        this.context = context;
        this.outEmail = outEmail;
        this.delegate = delegate;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pd = new ProgressDialog(context);
        pd.setMessage("Sending Email...");
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.show();
    }


    @Override
    protected String doInBackground(Void... params) {

        session = Session.getDefaultInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EmailUser.getEmailAddress(), EmailUser.getPassword());
            }
        });

        try {
            // Create new message object and add details
            Message mm = new MimeMessage(session);
            mm.setFrom(new InternetAddress((EmailUser.getEmailAddress()),
                    EmailUser.getFirstName() + " " + EmailUser.getLastName() ));

            // Add subject
            mm.setSubject(outEmail.getSubject());

            // Gets all the Outgoing Email recipients and adds them to the message
            recipients = outEmail.getRecipients();
            for(String r : recipients) {
                System.out.println("TO." + r +".");
                mm.addRecipients(Message.RecipientType.TO, InternetAddress.parse(r));
            }

            // Gets all the CC Email recipients and adds them to the message
            ccRecipients = outEmail.getCcRecipients();
            for(String c : ccRecipients){
                System.out.println("CC." + c +".");
                mm.addRecipients(Message.RecipientType.CC, InternetAddress.parse(c));
            }

            // Gets all the Bcc Email recipients and adds them to the message
            bccRecipients = outEmail.getBccRecipients();
            for(String b : bccRecipients){
                System.out.println("BCC." + b +".");
                mm.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(b));
            }



            Multipart multipart = new MimeMultipart();

            MimeBodyPart bodyPart = new MimeBodyPart();
            String thisMessage = outEmail.getMessage();

            // If this is a reply
            if(outEmail.getOriginalMessage() != null) {
                String replyMessage = outEmail.getOriginalMessage();

                // Append new message to original with details
                StringBuffer emailMessage = new StringBuffer(thisMessage);
                emailMessage.append("\r \n");
                emailMessage.append("<HR>");
                emailMessage.append("<b>From: </b>" + outEmail.getOriginalSender() + "<br/>");
                emailMessage.append("<b>Date: </b>" + outEmail.getOrignalDate().toString() + "<br/>");
                emailMessage.append("<b>Subject: </b>" + outEmail.getSubject() + "<br/>");
                emailMessage.append("\r \n");

                emailMessage.append(replyMessage);

                bodyPart.setContent(emailMessage.toString(), "text/html");
            }else{
                bodyPart.setContent(thisMessage.toString(), "text/html");
            }

            multipart.addBodyPart(bodyPart);

            // Boolean attachment check
            attachment = outEmail.getAttachment();
            // Add any attachments
            if(attachment == true){
                // Get attachments
                attachments = outEmail.getAttachments();
                for(int i=0; i<attachments.size(); i++){
                    MimeBodyPart attachPart = new MimeBodyPart();
                    File file = attachments.get(i);
                    String fileName = file.getName();

                    attachPart.attachFile(file);
                    System.out.println("ATTACHED FILE = " + fileName);
                    multipart.addBodyPart(attachPart);
                }
            }

            // Combine multipart email.
            mm.setContent(multipart);

            Transport.send(mm);
            System.out.println("EMAIL SENT");
        }
        catch (Exception e) {
            this.exception = e;
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        pd.dismiss();
        if(exception == null) {
            Toast.makeText(context, "Message sent!", Toast.LENGTH_LONG).show();
        }else{
            if(exception instanceof SendFailedException){
                Toast.makeText(context, "Error with recipients email address", Toast.LENGTH_LONG).show();
            }
        }
        // Alert calling method of finish
        result = FINISHED;
        delegate.sendEmailResult(result);
    }
}

