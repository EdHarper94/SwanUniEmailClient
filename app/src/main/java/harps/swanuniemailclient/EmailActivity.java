package harps.swanuniemailclient;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListView;
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
 * Singular Email Activity View
 * Created by eghar on 09/04/2017.
 */

public class EmailActivity extends Activity {

    private ImapSettings imapSettings = new ImapSettings();
    private Properties props = new Properties();

    private Folder inbox;
    private Session session;
    private Store store;
    private List<File> attachments = new ArrayList<File>();

    private Context context = EmailActivity.this;
    private GridView gridView;
    private AttachmentAdapter attachmentAdapter;

    ReceivedEmail email;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.email_view);

        // Get passed email
        Intent intent = getIntent();
        email = (ReceivedEmail) intent.getParcelableExtra("email");
        // DEBUG CODE //
        System.out.println(email);

        Button backButton = (Button)findViewById(R.id.back_button);
        Button downloadButton = (Button)findViewById(R.id.download_button);

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

        Log.d("ATTACHMENT", email.getAttachment().toString());

        // Pass email content to webview
        messageView.getSettings().setJavaScriptEnabled(true);
        messageView.loadDataWithBaseURL("", email.getText(), "text/html; charset=utf-8", "UTF-8", "");

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
    }

    public class downloadAttachment extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                // Init settings and inbox
                props = new ServerProperties().getInboxProperties();
                session = Session.getInstance(props, null);
                store = session.getStore("imaps");
                store.connect(imapSettings.getServerAddress(), EmailUser.getEmailAddress(), EmailUser.getPassword());

                inbox = store.getFolder("Inbox");
                UIDFolder uf = (UIDFolder) inbox;
                inbox.open(Folder.READ_WRITE);

                Long UID = email.getUID();
                Message message = uf.getMessageByUID(UID);

                // Get multipart of message
                Multipart multipart = (Multipart)message.getContent();

                // Loop through multipart to find the attachments
                for(int i=0; i<multipart.getCount(); i++){
                    Part bodyPart = multipart.getBodyPart(i);
                    if(!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())){
                        continue;
                    }

                    // DEBUG CODE //
                    System.out.println("WE HAVE AN ATTACHMENT");
                    // Save attachment

                    // Create temp file from bodybody part
                    //File file = File.createTempFile((bodyPart.getFileName()),null,context.getCacheDir());

                    File file = new File(context.getCacheDir() + "/" + bodyPart.getFileName());
                    System.out.println(file.toString());
                    // Save to internal storage
                    ((MimeBodyPart)bodyPart).saveFile(file);
                    attachments.add(file);
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

        @Override
        protected void onPostExecute(Void result) {
            // DEBUG CODE //
            for(int i=0; i<attachments.size(); i++){
                System.out.println("ON POST" + attachments.get(i));
                File file = attachments.get(i);

                //viewAttachment(context, file);

            }

            // DEBUG CODE
            File dir = context.getCacheDir();
            String files[] = dir.list();
            for(String s:files){
                System.out.println("FILE : " +s);
            }
            if(attachments!=null) {
                attachmentAdapter = new AttachmentAdapter(context, attachments);
                gridView.setAdapter(attachmentAdapter);


            }
        }
    }

    /**
     * Delete attachments from cache
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
     * Goes back to previous activity and delete attachments from cache
     */
    public void goBack(){

        if(email.getAttachment()== true){
            deleteFiles();
            attachments.clear();

            // DEBUG CODE //
            File dir = getApplicationContext().getCacheDir();
            String files[] = dir.list();
            for(String s:files){
                System.out.println("FILE : " +s);
            }

        }else{
            Toast.makeText(this, "ERROR CLEARING ATTACHMENTS", Toast.LENGTH_SHORT);
        }
        finish();
    }
}

