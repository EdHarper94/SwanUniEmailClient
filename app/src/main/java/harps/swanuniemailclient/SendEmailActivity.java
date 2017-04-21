package harps.swanuniemailclient;

import android.Manifest;
import android.app.Activity;
import android.content.Context;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Selection;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

/**
 * @file SendEmailActivity.java
 * @author Ed Harper
 * @date 29/03/2017
 *
 * Activity for creating an outgoing email and passing it to sendEmail
 * @see SendEmail
 *
 */

public class SendEmailActivity extends Activity {

    private final String WEBVIEW_UTF = "UTF-8";
    private final String WEBVIEW_SETTINGS = "text/html; charset=utf-8";

    private static final int CHOOSER_CODE = 1;

    private final String RECIPIENT_ERROR = "Please add a recipient.";
    private final String SUCCESS = "Y";

    private Context context = SendEmailActivity.this;

    // Edit texts
    private EditText toEditText, subjectEditText, messageEditText ,ccEditText, bccEditText;

    // Buttons
    private Button discardButton, attachmentButton, sendButton, addRecipientButton,
                    ccBccButton, addCcButton, addBccButton, deleteAttButton;

    private LinearLayout ccContainer;
    private LinearLayout bccContainer;
    private GridView attachmentGrid;
    private AttachmentAdapter attachmentAdapter;
    private WebView replyContent;
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
    private String originalMessage;
    private Date originalDate;
    private String originalSender;
    private Boolean attachment = false;

    private ReceivedEmail receivedEmail;
    private Boolean isReply = false;

    private ArrayList<File> attachments = new ArrayList<>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_email);


        // Init edit texts
        toEditText = (EditText)findViewById(R.id.to_edit_text);
        subjectEditText = (EditText)findViewById(R.id.subject_edit_text);
        messageEditText = (EditText)findViewById(R.id.message_edit_text);
        ccEditText = (EditText)findViewById(R.id.cc_edit_text);
        bccEditText = (EditText)findViewById(R.id.bcc_edit_text);

        // Init views
        ccContainer = (LinearLayout)findViewById(R.id.cc_linear_container);
        bccContainer = (LinearLayout)findViewById(R.id.bcc_container);
        attachmentGrid = (GridView)findViewById(R.id.attachment_grid);
        replyContent = (WebView)findViewById(R.id.reply_content);

        // Init buttons
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

        discardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                discardEmail();
            }
        });

        // Check whether this is a reply request
        Intent intent = getIntent();
        receivedEmail = intent.getParcelableExtra("email");
        if(receivedEmail!=null){
            isReply = true;
            prepareReply();
        }

    }

    /**
     * Adds replying data to UI
     */
    public void prepareReply(){
        toEditText.setText(receivedEmail.getFrom());
        subjectEditText.setText("Re: " + receivedEmail.getSubject());
        replyContent.getSettings().setJavaScriptEnabled(true);
        replyContent.loadDataWithBaseURL("", receivedEmail.getMessage(), WEBVIEW_SETTINGS , WEBVIEW_UTF, "");
    }


    /**
     * Select attachment using aFileChooser library.
     * Request required permission if not granted
     * @See aFileChooser library
     */
    public void selectAttachment(){
        Intent contentIntent = FileUtils.createGetContentIntent();

        Intent intent = Intent.createChooser(contentIntent, "Please select your file attachment.");

        int minSdk = Build.VERSION.SDK_INT;

        if(minSdk>=23){
            if(checkReadStoragePermission() == false){
                ActivityCompat.requestPermissions(SendEmailActivity.this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, CHOOSER_CODE);
            }
        }
        startActivityForResult(intent, CHOOSER_CODE);

    }

    /**
     * Gets permission request result. If granted allowed try again
     * @param requestCode
     * @param permissions
     * @param grantResults
     * @See aFileChooser library
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
     * @return permission check result
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
     * @param requestCode the activity code
     * @param resultCode the result of the activity
     * @param data the data passed back
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode == CHOOSER_CODE) {
            if(resultCode == RESULT_OK) {
                Uri uri = data.getData();
                String filePath = FileUtils.getPath(context, uri);
                if (filePath != null && FileUtils.isLocal(filePath)) {
                    File file = new File(filePath);
                    attachments.add(file);
                    displayAttachmentList();
                }
            }
        }else{
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

    // Deletes all selected attachments
    public void deleteAttachments(){
        attachment = false;
        attachmentGrid.setVisibility(View.GONE);
        deleteAttButton.setVisibility(View.GONE);
        attachments.clear();
        attachmentAdapter.notifyDataSetChanged();
    }


    /**
     * Gets the recipients from edit text field
     * @return the recipients list
     */
    public String getRecipientList(){
        recipientList = toEditText.getText().toString();
        return recipientList;
    }

    /**
     * Gets the cc recipients from edit text field
     * @return the cc recipient list
     */
    public String getCcList(){
        ccList = ccEditText.getText().toString();
        return ccList;
    }

    /**
     * Gets the bcc recipients from edit text field
     * @return the bcc recipients list
     */
    public String getBccList(){
        bccList = bccEditText.getText().toString();
        return bccList;
    }

    /**
     * Stores recipients and adds separator
     */
    public void addRecipient(){
        recipientList = getRecipientList();
        toEditText.setText(recipientList + "; ");
        Selection.setSelection(toEditText.getText(), toEditText.getText().length());
    }

    /**
     * Stores cc recipients and adds separator
     */
    public void addCcRecipients(){
        ccList = getCcList();
        System.out.println("ADD CC : " + recipientList);
        ccEditText.setText(ccList + "; ");
        Selection.setSelection(ccEditText.getText(), ccEditText.getText().length());
    }


    /**
     * Stores bcc recipients and adds separator
     */
    public void addBccRecipients(){
        bccEditText.setText(bccList + "; ");
        Selection.setSelection(bccEditText.getText(), bccEditText.getText().length());
    }

    /**
     * Final call to get recipients before sending
     * @return the final recipients array list
     */
    public ArrayList<String> getFinalRecipients(){
        String[] recipientsStrings = getRecipientList().split(";");

        for(String r : recipientsStrings){
            String recipient = r.trim();
            if(!recipient.isEmpty()) {
                recipients.add(recipient);
            }else{
            }
        }
        return recipients;
    }

    /**
     * Final call to get cc recipients before sending
     * @return the final cc recipients array list
     */
    public ArrayList<String> getFinalCcRecipients(){
        String[] ccRecipientStrings = getCcList().split(";");

        for(String r : ccRecipientStrings){
            String recipient = r.trim();
            if(!recipient.isEmpty()) {
                ccRecipients.add(recipient);
            }else{
            }
        }
        return ccRecipients;
    }

    /**
     * Final call to get cc recipients before sending
     * @return the final bcc recipients array list
     */
    public ArrayList<String> getFinalBccRecipients(){
        String[] bccRecipientStrings = getBccList().split(";");

        for(String r : bccRecipientStrings){
            String recipient = r.trim();
            if(!recipient.isEmpty()) {
                bccRecipients.add(recipient);
            }else{
            }
        }
        return bccRecipients;
    }

    /**
     * Gets email variables and passes them to AsyncTask for sending
     */
    public void send(){

        // Get Recipients
        recipients = getFinalRecipients();
        ccRecipients = getFinalCcRecipients();
        bccRecipients = getFinalBccRecipients();
        message = messageEditText.getText().toString();
        // Check whether is reply
        if(!isReply) {
            subject = subjectEditText.getText().toString();
            originalMessage = null;
            originalDate = null;
            originalSender = null;
        }else {
            originalMessage = receivedEmail.getMessage();
            originalDate = receivedEmail.getReceivedDate();
            originalSender = receivedEmail.getFrom();
        }



        // Init outgoing email object
        outEmail = new OutgoingEmail(null, subject, message, attachment, recipients, ccRecipients, bccRecipients, attachments, originalSender, originalMessage, originalDate);

        // If recipients have been added then send the email
        if(recipients!= null) {
            SendEmail se = new SendEmail(context, outEmail, new SendEmail.SendEmailResponse() {
                @Override
                public void sendEmailResult(String result) {
                    if(result.equals(SUCCESS)){
                        clearRecipientArrays();
                    }
                }
            });
            se.execute();
        }else{
            Toast.makeText(context, RECIPIENT_ERROR, Toast.LENGTH_LONG);
        }

    }

    /**
     * Discard the email and destroy activity
     */
    public void discardEmail(){
        finish();
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

