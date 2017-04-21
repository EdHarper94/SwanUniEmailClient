package harps.swanuniemailclient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.internet.MimeBodyPart;

/**
 * @file EmailActivity.java
 * @author Ed Harper
 * @date 09/04/2017
 *
 * Singular Email Activity View for viewing single email
 */

public class EmailActivity extends Activity {

    // Final variables
    private final String INBOX = "Inbox";
    private final String IMAPS = "imaps";
    private final String EMAIL = "email";

    // Server settings
    private final ServerSettings imapSettings = new ServerSettings();
    private Properties props = new Properties();
    private Folder inbox;
    private Session session;
    private Store store;

    private Context context = EmailActivity.this;

    private ReceivedEmail email;
    private List<File> attachments = new ArrayList<File>();

    private GridView gridView;
    private AttachmentAdapter attachmentAdapter;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.email_view);

        // Get passed email
        Intent intent = getIntent();
        email = (ReceivedEmail) intent.getParcelableExtra(EMAIL);
        // DEBUG CODE //
        System.out.println(email);

        Button backButton = (Button)findViewById(R.id.back_button);
        Button downloadButton = (Button)findViewById(R.id.download_button);
        Button replyButton = (Button)findViewById(R.id.reply_button);

        // Init views
        TextView fromView = (TextView)findViewById(R.id.from_view);
        TextView dateView = (TextView)findViewById(R.id.date_view);
        TextView subjectView = (TextView)findViewById(R.id.subject_view);
        WebView messageView = (WebView) findViewById(R.id.message_view);
        gridView = (GridView)findViewById(R.id.attachment_grid);

        // Add data to views
        dateView.setText(email.getReceivedDate().toString());
        fromView.setText(email.getFrom());
        subjectView.setText(email.getSubject());

        // Pass email content to webview
        messageView.getSettings().setJavaScriptEnabled(true);
        messageView.loadDataWithBaseURL("", email.getMessage(), "text/html; charset=utf-8", "UTF-8", "");

        new markRead().execute();

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goBack();
            }
        });

        // If email has attachment show download button
        if(email.getAttachment()== true){
            downloadButton.setVisibility(View.VISIBLE);
        }

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new downloadAttachment().execute();
            }
        });

        replyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start new intent
                Intent viewEmail = new Intent(context, SendEmailActivity.class);

                // Get Email to send to reply activity
                ReceivedEmail replyEmail = email;

                // Pass email to new activity
                viewEmail.putExtra(EMAIL, replyEmail);

                startActivity(viewEmail);
            }
        });
    }

    public class markRead extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... params) {

            try{
                props = new ServerProperties().getInboxProperties();
                session = Session.getInstance(props, null);
                store = session.getStore(IMAPS);
                store.connect(imapSettings.getServerAddress(), EmailUser.getEmailAddress(), EmailUser.getPassword());

                inbox = store.getFolder(INBOX);
                UIDFolder uf = (UIDFolder) inbox;
                inbox.open(Folder.READ_WRITE);

                Long UID = email.getUID();
                Message message = uf.getMessageByUID(UID);
                message.getContent();
                inbox.close(false);
            }catch(Exception e){

            }

            return null;
        }
    }

    public class downloadAttachment extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                // Init settings and inbox
                props = new ServerProperties().getInboxProperties();
                session = Session.getInstance(props, null);
                store = session.getStore(IMAPS);
                store.connect(imapSettings.getServerAddress(), EmailUser.getEmailAddress(), EmailUser.getPassword());

                inbox = store.getFolder(INBOX);
                UIDFolder uf = (UIDFolder) inbox;
                inbox.open(Folder.READ_WRITE);

                Long UID = email.getUID();
                Message message = uf.getMessageByUID(UID);

                // Get multipart of message
                Object object = (Multipart)message.getContent();

                // If string throw away
                if(object instanceof String){

                    String body = (String)object;
                }else if (object instanceof Multipart){
                    // Else if attachment download
                    Multipart multipart = (Multipart)object;
                    // Loop through multipart to find the attachments
                    for(int i=0; i<multipart.getCount(); i++){
                        Part bodyPart = multipart.getBodyPart(i);
                        if(!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())){
                            continue;
                        }

                        File file = new File(context.getCacheDir() + "/" + bodyPart.getFileName());
                        // Save to internal storage
                        ((MimeBodyPart)bodyPart).saveFile(file);
                        attachments.add(file);
                    }
                }

                inbox.close(false);
                store.close();
            }catch(MessagingException me){
                me.printStackTrace();
            }catch(IOException e){
                e.printStackTrace();
            }

            return null;
        }

        /**
         * If attachments are found update the UI
         * @param result
         */
        @Override
        protected void onPostExecute(Void result) {
            if(attachments!=null) {
                attachmentAdapter = new AttachmentAdapter(context, attachments);
                gridView.setAdapter(attachmentAdapter);
            }
        }
    }

    /**
     * Force delete attachments from cache
     * @return
     */
    public void deleteFiles(){
        File dir = context.getCacheDir();
        File files[] = dir.listFiles();
        for(File s:files){
            s.delete();
        }
    }

    /**
     * Passes UID back to calling activity to update read identifier
     */
    public void setRead(){
        Intent intent = new Intent();
        intent.putExtra("uid", email.getUID());
        setResult(RESULT_OK, intent);
    }

    /**
     * Goes back to previous activity and delete attachments from cache
     */
    public void goBack(){
        // If there are attachments
        if(email.getAttachment()== true){
            // Perform cleaning
            deleteFiles();
            attachments.clear();
        }
        setRead();
        finish();
    }
}

