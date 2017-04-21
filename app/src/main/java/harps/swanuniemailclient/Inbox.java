package harps.swanuniemailclient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.sun.mail.util.MailConnectException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

/**
 * @file Inbox.java
 * @author Ed Harper
 * @date 30/03/2017
 * @see InboxAdapter
 *
 * Inbox view. Grabs inbox messages from IMAP server and passes them to InboxAdapter
 */

public class Inbox extends Activity {

    private final int VIEW_EMAIL_REQUEST = 1;

    // Mime type final strings
    private final String TEXT = "text/*";
    private final String TEXT_PLAIN = "text/plain";
    private final String TEXT_HTML = "text/html";
    private final String MULTI = "multipart/*";
    private final String ALTERNATIVE = "multipart/alternative";
    private final String MIXED = "multipart/mixed";

    // Update final strings
    private final String MARK = "m";
    private final String DELETE = "d";

    // IMAP final settings
    private final String IMAPS = "imaps";
    private final String INBOX = "inbox";
    private final String DELETED_ITEMS = "Deleted Items";
    private final ServerSettings IMAP_SETTINGS = new ServerSettings();


    // Create Buttons
    private Button refreshButton, editButton, markButton, deleteButton, checkButton, createButton;

    private final String CONNECTION_ERROR = "Cannot connect to server, please try again later.";

    private Properties props = new Properties();

    private Folder inbox;
    private Session session;
    private Store store;

    private Message[] messages;
    private ArrayList<ReceivedEmail> emails = new ArrayList<>();

    private InboxAdapter ia;
    private ListView lv;
    private ProgressDialog pd;

    private static int startEmailDeductor = 9;
    private static int endEmailDeductor = 0;
    private boolean refresh = false;
    private boolean allChecked = false;

    private Boolean editButtonsShowing = false;

    private Context context;

    private boolean textIsHtml = false;

    public Inbox(){
        this.context = Inbox.this;
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inbox);

        lv = (ListView) findViewById(R.id.email_list);
        refreshButton = (Button) findViewById(R.id.refresh_button);
        editButton = (Button) findViewById(R.id.edit_button);
        markButton = (Button) findViewById(R.id.mark_button);
        deleteButton = (Button) findViewById(R.id.delete_button);
        checkButton = (Button)findViewById(R.id.check_all_button);
        createButton = (Button)findViewById(R.id.create_email_button);

        new getEmails().execute(refresh);

        lv.setOnScrollListener(new EndlessScrollListener() {
            @Override
            public boolean onLoadMore(int page, int totalItemsCount) {

                new getEmails().execute(refresh);
                return true;

            }
        });

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

                // Start new intent
                Intent viewEmail = new Intent(context, EmailActivity.class);

                // Get selected email
                ReceivedEmail email = (ReceivedEmail) lv.getItemAtPosition(position);

                // Pass email to new activity
                viewEmail.putExtra("email", email);

                startActivityForResult(viewEmail, VIEW_EMAIL_REQUEST);
            }
        });

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent createEmail = new Intent(context, SendEmailActivity.class);
                startActivity(createEmail);
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh = true;
                new getEmails().execute(refresh);
                refresh = false;
                // Result list view
                lv.setSelection(0);
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ia.setShowCheckboxes();
                ia.notifyDataSetChanged();
                showEditButtons(markButton, deleteButton, checkButton);
            }
        });

        markButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get checked checkboxes
                new ChangeEmailStatus(ia.getEmailUIDs(),MARK).execute();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ChangeEmailStatus(ia.getEmailUIDs(), DELETE).execute();
            }
        });

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!allChecked) {
                    allChecked = true;
                    ia.toggleAllCheckboxes(allChecked);
                    ia.notifyDataSetChanged();
                }else{
                    allChecked = false;
                    ia.toggleAllCheckboxes(allChecked);
                    ia.notifyDataSetChanged();
                }
            }
        });
    }

    /**
     * Once email has been viewed updates the unread status + notifies adapter
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == VIEW_EMAIL_REQUEST && resultCode == RESULT_OK){
            Long UID = data.getLongExtra("uid", 0);

            // Toggle unread status
            toggleUnread(UID, false);

            // Notify adapter
            ia.notifyDataSetChanged();
        }
    }

    /**
     * Shows and hides checkboxes, mark & delete buttons
     */
    public void showEditButtons(Button markButton, Button deleteButton, Button checkButton){
        if(editButtonsShowing){
            editButtonsShowing = false;
            checkButton.setVisibility(View.GONE);
            markButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }else{
            editButtonsShowing = true;
            checkButton.setVisibility(View.VISIBLE);
            markButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Incremements the email deductors
     */
    public void incrementDeductors(){
        this.startEmailDeductor +=10;
        this.endEmailDeductor +=10;
    }

    /**
     * Checks whether request is a refresh.
     * If refresh deductors are reset and emails cleared.
     * Else continue getting new emails
     * @param isRefresh
     * @param totalMessages
     * @return
     * @throws MessagingException
     */
    public Message[] isRefresh(Boolean isRefresh, int totalMessages) throws MessagingException {
        // If refresh request
        if(isRefresh == true){
            // Set deductors to default
            startEmailDeductor = 9;
            endEmailDeductor = 0;
            emails.clear();
            messages =  inbox.getMessages(totalMessages - startEmailDeductor, totalMessages);
        }else {
            // Get emails in inbox
            messages = inbox.getMessages(totalMessages - startEmailDeductor, totalMessages - endEmailDeductor);
        }
        return messages;
    }

    /**
     * Gets text body from multipart message
     * Source: http://www.oracle.com/technetwork/java/javamail/faq/index.html#mainbody
     * Return the primary text content of the message.
     */
    /**
     * Gets text body from multipart message
     * @see <http://www.oracle.com/technetwork/java/javamail/faq/index.html#mainbody></http://www.oracle.com/technetwork/java/javamail/faq/index.html#mainbody>
     * @param p the passed part
     * @return the primary text content of the message
     * @throws MessagingException
     * @throws IOException
     */
    private String getText(Part p) throws MessagingException, IOException {
        if (p.isMimeType(TEXT)) {
            String s = (String)p.getContent();
            textIsHtml = p.isMimeType(TEXT_HTML);
            return s;
        }

        if (p.isMimeType(ALTERNATIVE)) {
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType(TEXT_PLAIN)) {
                    if (text == null)
                        text = getText(bp);
                    continue;
                } else if (bp.isMimeType(TEXT_HTML)) {
                    String s = getText(bp);
                    if (s != null)
                        return s;
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType(MULTI)) {
            Multipart mp = (Multipart)p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }

        return null;
    }

    /**
     * Checks whether message (email) has an attachment
     * http://www.oracle.com/technetwork/java/javamail/faq/index.html#hasattach
     * @param message
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    public boolean hasAttachments(Message message) throws MessagingException, IOException {
        if (message.isMimeType(MIXED)) {
            Multipart multipart = (Multipart)message.getContent();
            if (multipart.getCount() > 1)
                return true;
        }
        return false;
    }

    /**
     * Gets emails from server via background thread and then passes them to the UI thread
     */
    public class getEmails extends AsyncTask<Boolean, Void, Void> {

        /**
         * Progress dialog for fetching emails
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(context);
            pd.setMessage("Fetching Emails...");
            pd.setCancelable(false);
            pd.setCanceledOnTouchOutside(false);
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.show();
        }

        @Override
        protected Void doInBackground(Boolean... params) {

            props = new ServerProperties().getInboxProperties();
            Boolean isRefresh = params[0];

            try {
                // Connect
                session = Session.getInstance(props, null);
                store = session.getStore(IMAPS);
                store.connect(IMAP_SETTINGS.getServerAddress(), EmailUser.getEmailAddress(), EmailUser.getPassword());

                // Get folder
                inbox = store.getFolder(INBOX);
                UIDFolder uf = (UIDFolder)inbox;
                inbox.open(Folder.READ_ONLY);

                int totalMessages = inbox.getMessageCount();

                messages = isRefresh(isRefresh, totalMessages);

                // Go through emails newest to oldest
                for (int i = messages.length - 1; i >= 0; i--) {
                    Message message = messages[i];

                    // Get data from messages
                    Long UID = uf.getUID(message);
                    String from = message.getFrom()[0].toString();
                    Date date = message.getReceivedDate();
                    String subject = message.getSubject();

                    // GET MESSAGE CONTENT
                    String text = getText(message);
                    Boolean attachment = hasAttachments(message);
                    Boolean unread = true;
                    if(message.isSet(Flags.Flag.SEEN)){
                        unread = false;
                    }

                    // Create email
                    ReceivedEmail email = new ReceivedEmail(UID , subject, text, attachment, from, date, unread);
                    // Store to array
                    emails.add(email);
                }
                inbox.close(false);
                store.close();

                // Finally increment deductors
                incrementDeductors();

            }catch (IOException e){
                e.printStackTrace();
            }catch (MessagingException me) {
                me.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            // Check emails have been downloaded
            if(emails != null){
                // Check array adapter has been initialised
                if(ia == null) {
                    // If there is no Adapter create one and pass emails
                    ia = new InboxAdapter(context, emails);
                    lv.setAdapter(ia);
                }else{
                    // Clear checkbox arrays
                    ia.clearEmailUIDs();
                    ia.clearSelectedPos();

                    // Notify Adapter that data has changed
                    ia.notifyDataSetChanged();
                }
            }
            // Dismiss progress dialog
            if(pd.isShowing()){
                pd.dismiss();
            }
        }

    }


    /**
     * Alters passed emails statuses to deleted or unread/read on server.
     */
    public class ChangeEmailStatus extends AsyncTask<Void, Void, Void>{
        private Exception exception;
        private List<Long> checkedEmails;
        private String type;
        /**
         * Initialises changeEmailStatus with checked emails and the type of update to execute
         * @param checkedEmails
         * @param type
         */
        public ChangeEmailStatus(List<Long> checkedEmails, String type){
            this.checkedEmails = checkedEmails;
            this.type = type;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(context);
            pd.setMessage("Updating Emails...");
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... result) {

            props = new ServerProperties().getInboxProperties();
            try {
                // Connect
                session = Session.getInstance(props, null);
                store = session.getStore(IMAPS);
                store.connect(IMAP_SETTINGS.getServerAddress(), EmailUser.getEmailAddress(), EmailUser.getPassword());

                // Get folder
                inbox = store.getFolder(INBOX);
                UIDFolder uf = (UIDFolder) inbox;
                inbox.open(Folder.READ_WRITE);

                // Loop through selected emails
                for(int i=0; i<checkedEmails.size(); i++){
                    Long UID = checkedEmails.get(i);
                    Message message = uf.getMessageByUID(UID);
                    Boolean unread;
                    Boolean seen;

                    // If message is mark request
                    if(type.equals(MARK)) {
                        // If message is marked seen then set to unread
                        if (message.isSet(Flags.Flag.SEEN)) {
                            unread = true;
                            seen = false;
                            message.setFlag(Flags.Flag.SEEN, seen);
                            toggleUnread(UID, unread);
                        } else {
                            // Message is not seen so set to unread
                            unread = false;
                            seen = true;
                            message.setFlag(Flags.Flag.SEEN, seen);
                            toggleUnread(UID, unread);
                        }
                    }
                    // Else if delete request
                    else if(type.equals(DELETE)){
                        Boolean deleted = true;
                        // Get Deleted Items Folder
                        Folder deletedItems = store.getFolder(DELETED_ITEMS);
                        // Move email/s to Folder
                        inbox.copyMessages(new Message[]{message}, deletedItems);
                        // Delete from Inbox
                        message.setFlag(Flags.Flag.DELETED, deleted);
                        markDeleted(UID);

                    }
                }
                inbox.close(true);
                store.close();

            }catch(MailConnectException me){
                this.exception = me;
            } catch (NoSuchProviderException spe) {
                this.exception = spe;
            } catch (MessagingException me) {
                this.exception = me;
            }

            return null;
        }

        /**
         * Updates UI on UI thread
         * @param result
         */
        protected void onPostExecute(Void result){
            if(exception!=null){
                Toast.makeText(Inbox.this, CONNECTION_ERROR, Toast.LENGTH_LONG).show();
            }
            // If request was deletion clear checkbox arrays
            if(type.equals(DELETE)) {
                ia.clearEmailUIDs();
                ia.clearSelectedPos();
            }
            ia.notifyDataSetChanged();

            if(pd.isShowing()){
                pd.dismiss();
            }
        }
    }

    /**
     * Toggles the unread status of the passed email
     * @param UID the uid of the email to toggle
     * @param unread the status of unread
     */
    public void toggleUnread(Long UID, Boolean unread){
        for(int i=0; i<emails.size(); i++){
            if(emails.get(i).getUID() == UID){
                emails.get(i).setUnread(unread);
            }
        }
    }

    /**
     * Removes email from emails array
     * @param UID the uid of the email to delete
     */
    public void markDeleted(Long UID){
        for(int i=0;i<emails.size(); i++){
            if(emails.get(i).getUID() == UID){
                emails.remove(i);
            }
        }
    }

}