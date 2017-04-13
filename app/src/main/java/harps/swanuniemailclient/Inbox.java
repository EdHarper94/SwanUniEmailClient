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
 *
 * Inbox view. Grabs inbox messages from IMAP server and passes them to InboxAdapter
 * @see InboxAdapter
 *
 * Created by eghar on 30/03/2017.
 */

public class Inbox extends Activity {

    private ImapSettings imapSettings = new ImapSettings();
    private Properties props = new Properties();

    private Folder inbox;
    private Session session;
    private Store store;

    private Message[] messages;
    private ArrayList<ReceivedEmail> emails = new ArrayList<>();

    private InboxAdapter ia;
    private ListView lv;
    private ProgressDialog pd;

    static int startEmailDeductor = 9;
    static int endEmailDeductor = 0;
    private boolean refresh = false;

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
        Button refreshButton = (Button) findViewById(R.id.refresh_button);
        Button editButton = (Button) findViewById(R.id.edit_button);
        final Button markButton = (Button) findViewById(R.id.mark_button);
        final Button deleteButton = (Button) findViewById(R.id.delete_button);

        System.out.print("IN ON CREATE");
        new getEmails().execute(refresh);

        lv.setOnScrollListener(new EndlessScrollListener() {
            @Override
            public boolean onLoadMore(int page, int totalItemsCount) {
                System.out.println("LOADDD MOREEEE");
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

                startActivity(viewEmail);
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh = true;
                new getEmails().execute(refresh);
                refresh = false;
                lv.setSelection(1);
            }
        });

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ia.setShowCheckboxes();
                ia.notifyDataSetChanged();
                showEditButtons(markButton, deleteButton);
            }
        });

        markButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get checked checkboxes

                new ChangeEmailStatus(ia.emailUIDs,"m").execute();
                System.out.println("Mark");
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ChangeEmailStatus(ia.emailUIDs, "d").execute();
                System.out.println("Delete");
            }
        });
    }

    // Shows and hides checkboxes and mark & delete buttons
    public void showEditButtons(Button markButton, Button deleteButton){
        if(editButtonsShowing){
            editButtonsShowing = false;
            markButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }else{
            editButtonsShowing = true;
            markButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
        }
    }

    // Increments deductors
    public void incrementDeductors(){
        this.startEmailDeductor +=10;
        this.endEmailDeductor +=10;
    }

    // Detects whether request is a refresh or continued loading request
    public Message[] isRefresh(Boolean isRefresh, int totalMessages) throws MessagingException {
        // If refresh request
        if(isRefresh == true){
            System.out.println("Refresh TRUE ***********************");
            // Set deductors to default
            startEmailDeductor = 9;
            endEmailDeductor = 0;
            emails.clear();
            messages =  inbox.getMessages(totalMessages - startEmailDeductor, totalMessages);
        }else {
            System.out.println("Refresh FALSE********************");
            // Get emails in inbox
            messages = inbox.getMessages(totalMessages - startEmailDeductor, totalMessages - endEmailDeductor);
        }
        return messages;
    }

    /**
     * Source: http://www.oracle.com/technetwork/java/javamail/faq/index.html#mainbody
     * Return the primary text content of the message.
     */
    private String getText(Part p) throws MessagingException, IOException {
        if (p.isMimeType("text/*")) {
            String s = (String)p.getContent();
            textIsHtml = p.isMimeType("text/html");
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart)p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text = getText(bp);
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText(bp);
                    if (s != null)
                        return s;
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
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
     * http://www.oracle.com/technetwork/java/javamail/faq/index.html#hasattach
     * @param message
     * @return
     * @throws MessagingException
     * @throws IOException
     */
    public boolean hasAttachments(Message message) throws MessagingException, IOException {
        if (message.isMimeType("multipart/mixed")) {
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
                session = Session.getInstance(props, null);
                store = session.getStore("imaps");
                store.connect(imapSettings.getServerAddress(), EmailUser.getEmailAddress(), EmailUser.getPassword());

                inbox = store.getFolder("Inbox");
                UIDFolder uf = (UIDFolder)inbox;
                inbox.open(Folder.READ_ONLY);

                // **** DEBUG CODE **** //
                System.out.println("# of Undread Messages : " + inbox.getUnreadMessageCount());

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

                // **** DEBUG CODE **** //
                System.out.println("MESSAGES DOWNLOADED "+refresh);

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
            // **** DEBUG CODE **** //
            System.out.println("EMAIL COUNT: " + emails.size());
            for (int i = 0; i < emails.size(); i++) {
                System.out.println(emails.get(i));
            }
            if(emails != null){
                if(ia == null) {
                    // If there is no Adapter create one and pass emails
                    ia = new InboxAdapter(context, emails);
                    lv.setAdapter(ia);
                }else{
                    // Notify Adapter that data has changed
                    ia.notifyDataSetChanged();
                }
            }
            if(pd.isShowing()){
                pd.dismiss();
            }
        }

    }


    /**
     * Alters passed emails statuses to deleted or unread/read.
     */
    public class ChangeEmailStatus extends AsyncTask<Void, Void, Void>{
        List<Long> checkedEmails;
        String type;


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
            System.out.println(checkedEmails);
            System.out.println("In do in background");
            try {
                session = Session.getInstance(props, null);
                store = session.getStore("imaps");
                store.connect(imapSettings.getServerAddress(), EmailUser.getEmailAddress(), EmailUser.getPassword());

                inbox = store.getFolder("Inbox");
                UIDFolder uf = (UIDFolder) inbox;
                inbox.open(Folder.READ_WRITE);

                for(int i=0; i<checkedEmails.size(); i++){
                    System.out.println("E LIST" + checkedEmails.get(i));
                    Long UID = checkedEmails.get(i);
                    Message message = uf.getMessageByUID(UID);
                    Boolean unread;
                    Boolean seen;

                    // If message is mark request
                    if(type.equals("m")) {
                        // If message is marked seen then set to unread
                        if (message.isSet(Flags.Flag.SEEN)) {
                            unread = true;
                            seen = false;
                            message.setFlag(Flags.Flag.SEEN, seen);
                            toggleUnread(UID, unread);
                            System.out.println(" 1    UID: " + UID + ". Unread: " + unread);
                        } else {
                            // Message is not seen so set to unread
                            unread = false;
                            seen = true;
                            message.setFlag(Flags.Flag.SEEN, seen);
                            toggleUnread(UID, unread);
                            System.out.println(" 2    UID: " + UID + ". Unread: " + unread);
                        }
                    }
                    // Else if delete request
                    else if(type.equals("d")){
                        Boolean deleted = true;
                        // Get Deleted Items Folder
                        Folder deletedItems = store.getFolder("Deleted Items");
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
                me.printStackTrace();
                Toast.makeText(Inbox.this, "UNABLE TO CONNECT", Toast.LENGTH_LONG).show();
            } catch (NoSuchProviderException e1) {
                e1.printStackTrace();
            } catch (MessagingException e1) {
                e1.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(Void result){
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