package harps.swanuniemailclient;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Selection;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.SearchException;
import javax.sql.DataSource;

/**
 * Class to send email via University IMAP server
 *
 * @author Edward Harper
 * @date 29/03/17
 */

public class SendEmail extends Activity {

    private Context context = SendEmail.this;

    private EditText toEditText;
    private EditText subjectEditText;
    private EditText messageEditText;
    private EditText ccEditText;
    private EditText bccEditText;

    private Button discardButton;
    private Button attachmentButton;
    private Button sendButton;
    private Button addRecipientButton;
    private Button ccBccButton;
    private Button addCcButton;
    private Button addBccButton;
    private Button deleteAttButton;

    private LinearLayout ccContainer;
    private LinearLayout bccContainer;
    private GridView attachmentGrid;
    private AttachmentAdapter attachmentAdapter;
    private Boolean showCarbonCopies = false;

    private OutgoingEmail outEmail;

    private ArrayList<String> recipients = new ArrayList<>();
    private ArrayList<String> ccRecipients = new ArrayList<>();
    private ArrayList<String> bccRecipients = new ArrayList<>();
    private String recipientList = "";
    private String ccList = "";
    private String bccList = "";
    private String subject;
    private String message;
    private Boolean attachment = false;

    private ArrayList<File> attachments = new ArrayList<>();

    private static final int CHOOSER_CODE = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_email);

        toEditText = (EditText)findViewById(R.id.to_edit_text);
        subjectEditText = (EditText)findViewById(R.id.subject_edit_text);
        messageEditText = (EditText)findViewById(R.id.message_edit_text);
        ccEditText = (EditText)findViewById(R.id.cc_edit_text);
        bccEditText = (EditText)findViewById(R.id.bcc_edit_text);

        ccContainer = (LinearLayout)findViewById(R.id.cc_linear_container);
        bccContainer = (LinearLayout)findViewById(R.id.bcc_container);
        attachmentGrid = (GridView)findViewById(R.id.attachment_grid);


        discardButton = (Button)findViewById(R.id.discard_button);
        addRecipientButton = (Button)findViewById(R.id.add_recipient_button);
        ccBccButton = (Button)findViewById(R.id.cc_bcc_button);
        addCcButton = (Button)findViewById(R.id.add_cc_recipient_button);
        addBccButton = (Button)findViewById(R.id.add_bcc_recipient_button);
        attachmentButton = (Button)findViewById(R.id.add_attachment_button);
        deleteAttButton = (Button)findViewById(R.id.delete_attachment_button);
        sendButton = (Button)findViewById(R.id.send_button);

        addRecipientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addRecipient();
            }
        });

        ccBccButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!showCarbonCopies) {
                    ccContainer.setVisibility(View.VISIBLE);
                    bccContainer.setVisibility(View.VISIBLE);
                    showCarbonCopies = true;
                }else{
                    ccContainer.setVisibility(View.GONE);
                    bccContainer.setVisibility(View.GONE);
                    showCarbonCopies = false;
                }

            }
        });

        addCcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addCcRecipients();
            }
        });

        addBccButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addBccRecipients();
            }
        });

        attachmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectAttachment();
            }
        });

        deleteAttButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAttachments();
            }});

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send();
            }
        });

    }

    /**
     * Select attachment using aFileChooser library.
     * Request required permission if not granted
     * @See aFileChooser
     */
    public void selectAttachment(){
        Intent contentIntent = FileUtils.createGetContentIntent();

        Intent intent = Intent.createChooser(contentIntent, "Please select your file attachment.");

        int minSdk = Build.VERSION.SDK_INT;

        if(minSdk>=23){
            if(checkReadStoragePermission() == false){
                ActivityCompat.requestPermissions(SendEmail.this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, CHOOSER_CODE);
            }
        }
        startActivityForResult(intent, CHOOSER_CODE);

    }

    /**
     * Gets permission request result
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == CHOOSER_CODE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectAttachment();
            } else {
                Log.d("PERMISSION", requestCode + "Permission Not Granted" );
            }
        }
    }

    /**
     * Checks required permission is granted
     * @return
     */
    public boolean checkReadStoragePermission(){
        int permission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        if(permission != PackageManager.PERMISSION_GRANTED){
            return false;
        }else{
            return true;
        }
    }

    /**
     * Once file has been selected save it
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == CHOOSER_CODE) {
            System.out.println("REQUEST OKAY");
            if(resultCode == RESULT_OK) {
                System.out.println(requestCode);
                Uri uri = data.getData();
                String filePath = FileUtils.getPath(context, uri);

                if (filePath != null && FileUtils.isLocal(filePath)) {
                    File file = new File(filePath);
                    attachments.add(file);
                    displayAttachmentList();
                }

                System.out.println("FILE = " + filePath);
            }
        }else{
            System.out.println("ERROR");
        }
    }

    /**
     * Displays Attachments to screen
     */
    public void displayAttachmentList(){
        attachment = true;
        attachmentGrid.setVisibility(View.VISIBLE);
        deleteAttButton.setVisibility(View.VISIBLE);
        System.out.println("IN HEREEEEE");
        if(attachmentAdapter == null) {
            attachmentAdapter = new AttachmentAdapter(context, attachments);
            attachmentGrid.setAdapter(attachmentAdapter);
        }else{
            attachmentAdapter.notifyDataSetChanged();
        }
    }

    public void deleteAttachments(){
        attachment = false;
        attachmentGrid.setVisibility(View.GONE);
        deleteAttButton.setVisibility(View.GONE);
        attachments.clear();
        attachmentAdapter.notifyDataSetChanged();
    }


    /**
     * Gets the recipients from "to_edit_text"
     * @return
     */
    public String getRecipientList(){
        recipientList = toEditText.getText().toString();
        return recipientList;
    }

    public String getCcList(){
        ccList = ccEditText.getText().toString();
        return ccList;
    }

    public String getBccList(){
        bccList = bccEditText.getText().toString();
        return bccList;
    }

    /**
     * Stores recipients
     */
    public void addRecipient(){
        recipientList = getRecipientList();
        System.out.println("ADD RECIP : " + recipientList);
        toEditText.setText(recipientList + "; ");
        Selection.setSelection(toEditText.getText(), toEditText.getText().length());
    }

    /**
     * Stores cc recipients
     */
    public void addCcRecipients(){
        ccList = getCcList();
        System.out.println("ADD CC : " + recipientList);
        ccEditText.setText(ccList + "; ");
        Selection.setSelection(ccEditText.getText(), ccEditText.getText().length());
    }


    /**
     * Stores bcc recipients
     */
    public void addBccRecipients(){
        bccList = getBccList();
        System.out.println("ADD CC : " + bccList);
        bccEditText.setText(bccList + "; ");
        Selection.setSelection(bccEditText.getText(), bccEditText.getText().length());
    }

    /**
     * Final call to get recipients before sending
     * @return
     */
    public ArrayList<String> getFinalRecipients(){
        String[] recipientsStrings = getRecipientList().split(";");

        for(String r : recipientsStrings){
            String recipient = r.trim();
            if(!recipient.isEmpty()) {
                recipients.add(recipient);
                System.out.println(recipient);
            }else{
                System.out.println("IS EMPTY");
            }
        }
        return recipients;
    }

    /**
     * Final call to get cc recipients before sending
     * @return
     */
    public ArrayList<String> getFinalCcRecipients(){
        String[] ccRecipientStrings = getCcList().split(";");

        for(String r : ccRecipientStrings){
            String recipient = r.trim();
            if(!recipient.isEmpty()) {
                ccRecipients.add(recipient);
                System.out.println(recipient);
            }else{
                System.out.println("IS EMPTY");
            }
        }
        return ccRecipients;
    }

    /**
     * Final call to get cc recipients before sending
     * @return
     */
    public ArrayList<String> getFinalBccRecipients(){
        String[] bccRecipientStrings = getBccList().split(";");

        for(String r : bccRecipientStrings){
            String recipient = r.trim();
            if(!recipient.isEmpty()) {
                bccRecipients.add(recipient);
                System.out.println(recipient);
            }else{
                System.out.println("IS EMPTY");
            }
        }
        return bccRecipients;
    }

    /**
     * Initialises email variables and passes them to AsyncTask
     */
    public void send(){

        recipients = getFinalRecipients();
        ccRecipients = getFinalCcRecipients();
        bccRecipients = getFinalBccRecipients();

        for(String s : recipients){
            System.out.println(s);
        }

        subject = subjectEditText.getText().toString();
        message = messageEditText.getText().toString();

        // If there are no carbon copy recipient
        outEmail = new OutgoingEmail(null, subject, message, attachment, recipients, ccRecipients, bccRecipients);


        if(recipients!= null) {
            new sendEmail().execute();
        }else{
            Toast.makeText(context, "Please Enter a recipient.", Toast.LENGTH_LONG);
        }

    }

    public class sendEmail extends AsyncTask<Void, Void, Void> {
        private Exception exception;
        private Session session;

        private ProgressDialog pd;

        // Init IMAP Settings
        private ImapSettings imapSettings = new ImapSettings();
        private Properties props = new ServerProperties().getSMTPProperties();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(context);
            pd.setMessage("Sending Email...");
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.show();
        }


        @Override
        protected Void doInBackground(Void... params) {

            session = Session.getDefaultInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(EmailUser.getEmailAddress(), EmailUser.getPassword());
                }
            });


            System.out.println("PASSWORD AUTHENTICATED");
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

                MimeBodyPart bodyPart = new MimeBodyPart();

                // Add email text
                bodyPart.setContent(outEmail.getMessage(), "text/html");

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(bodyPart);

                // Add any attachments
                if(attachment == true){
                    for(int i=0; i<attachments.size(); i++){
                        MimeBodyPart attachPart = new MimeBodyPart();
                        File file = attachments.get(i);
                        String fileName = file.getName();

                        attachPart.attachFile(file);
                        System.out.println("ATTACHED FILE = " + fileName);
                        multipart.addBodyPart(attachPart);
                    }
                }

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
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            pd.dismiss();
            if(exception == null) {
                Toast.makeText(context, "Message sent!", Toast.LENGTH_LONG).show();
            }else{
                if(exception instanceof SendFailedException){
                    Toast.makeText(context, "Error with recipients email address", Toast.LENGTH_LONG).show();
                }
            }
            clearRecipientArrays();
        }
    }

    /**
     * Tidying arrays
     */
    public void clearRecipientArrays(){
        recipients.clear();
        ccRecipients.clear();
        bccRecipients.clear();
    }
}

