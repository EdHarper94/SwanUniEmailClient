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
import javax.mail.MessagingException;
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
 * @file SendEmail.java
 * @author Ed Harper
 * @date 17/04/2017
 *
 * Send Email task. Sends email on background thread using SMTP.
 */

public class SendEmail extends AsyncTask<Void, Void, String> {

    /**
     * Interface to receive result of send email async task
     */
    public interface SendEmailResponse{
        void sendEmailResult(String result);
    }

    public SendEmailResponse delegate = null;


    private final String FINISHED = "Y";
    public final String SUCCESS = "Message sent!";
    public final String RECIPIENT_ERROR = "Error with recipients email address";
    public final String CONNECTION_ERROR = "Unable to send email, please try again later.";

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

    /**
     * Initialises Send email with passed context and outEmail
     * @param context passed context
     * @param outEmail the email to be sent
     * @param delegate the result interface
     */
    public SendEmail(Context context, OutgoingEmail outEmail, SendEmailResponse delegate){
        this.context = context;
        this.outEmail = outEmail;
        this.delegate = delegate;
    }

    /**
     * UI progress dialog initialise
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        pd = new ProgressDialog(context);
        pd.setMessage("Sending Email...");
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.show();
    }


    /**
     * Orders email fields and sends via SMTP on the background network thread
     * @param params none
     * @return null
     */
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
                mm.addRecipients(Message.RecipientType.TO, InternetAddress.parse(r));
            }

            // Gets all the CC Email recipients and adds them to the message
            ccRecipients = outEmail.getCcRecipients();
            for(String c : ccRecipients){
                mm.addRecipients(Message.RecipientType.CC, InternetAddress.parse(c));
            }

            // Gets all the Bcc Email recipients and adds them to the message
            bccRecipients = outEmail.getBccRecipients();
            for(String b : bccRecipients){
                mm.addRecipients(Message.RecipientType.BCC, InternetAddress.parse(b));
            }

            Multipart multipart = new MimeMultipart();

            MimeBodyPart bodyPart = getMessageBodyPart();

            // Add to multipart
            multipart.addBodyPart(bodyPart);

            // Boolean attachment check
            attachment = outEmail.getAttachment();
            // Add any attachments
            if(attachment == true){
                // Get attachments
                attachments = outEmail.getAttachments();
                // Loop through attachments
                for(int i=0; i<attachments.size(); i++){
                    MimeBodyPart attachPart = new MimeBodyPart();
                    File file = attachments.get(i);
                    String fileName = file.getName();

                    // Add files to attachment part
                    attachPart.attachFile(file);
                    // Add attachment to email
                    multipart.addBodyPart(attachPart);
                }
            }

            // Combine multipart email.
            mm.setContent(multipart);

            Transport.send(mm);
        }
        catch (Exception e) {
            this.exception = e;
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Checks for exceptions and notifies user of success/failure.
     * and passes result back to calling method
     * @param result
     */
    @Override
    protected void onPostExecute(String result) {
        pd.dismiss();
        if(exception == null) {
            Toast.makeText(context, SUCCESS, Toast.LENGTH_LONG).show();
        }else{
            if(exception instanceof SendFailedException){
                Toast.makeText(context, RECIPIENT_ERROR, Toast.LENGTH_LONG).show();
            }
            else{
                Toast.makeText(context, CONNECTION_ERROR, Toast.LENGTH_LONG).show();
            }
        }
        // Alert calling method of finish
        result = FINISHED;
        delegate.sendEmailResult(result);
    }

    /**
     * Checks whether is reply and creates message part
     * @return the message mime body part
     * @throws MessagingException
     */
    public MimeBodyPart getMessageBodyPart() throws MessagingException{
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
        return bodyPart;
    }
}

