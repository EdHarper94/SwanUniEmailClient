package harps.swanuniemailclient;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


/**
 * @file InboxAdapter.java
 * @author Ed Harper
 * @date 31/03/2017
 *
 * Inbox adpater for inflating email views and adding data
 */

public class InboxAdapter extends BaseAdapter{

    private Context context;
    private ArrayList<ReceivedEmail> emails = new ArrayList<>();
    private LayoutInflater inflater;
    private List<Integer> selectedPos;
    private List<Long> emailUIDs;
    private Boolean showCheckboxes;

    /**
     * Initialises inbox adapter with passed context and emails arraylist
     * @param context the passed context
     * @param emails the emails to add to the view
     */
    public InboxAdapter(Context context, ArrayList<ReceivedEmail> emails){
        this.context = context;
        this.emails = emails;
        this.inflater = (LayoutInflater.from(context));
        this.selectedPos = new ArrayList<>();
        this.emailUIDs = new ArrayList<>();
        this.showCheckboxes = false;
    }

    /**
     * The total number of emails
     * @return
     */
    public int getCount(){
        return emails.size();
    }

    /**
     * Gets the email at position
     * @param position the position
     * @return
     */
    public Object getItem(int position){
        return emails.get(position);
    }

    /**
     * Ges the row id for specified position
     * @param position
     * @return
     */
    public long getItemId(int position){
        return position;
    }

    /**
     * Gets the selected checkboxes
     * @return List of the selected checkbox positions
     */
    public List<Integer> getSelectedPos(){
        return selectedPos;
    }

    /**
     * Gets the selected emails
     * @return List of the selected email UIDs
     */
    public List<Long> getEmailUIDs(){
        return emailUIDs;
    }

    /**
     * Used to clear the checkbox selected pos array
     */
    public void clearSelectedPos(){
        this.selectedPos.clear();
    }

    /**
     * Used to clear the checkbox email UIDs array
     */
    public void clearEmailUIDs(){
        this.emailUIDs.clear();
    }

    /**
     * Holds items for view recycling
     */
    public class ViewHolder{
        public TextView fromText;
        public TextView dateText;
        public TextView subjectText;
        public ImageView unreadImage;
        public boolean unread;
        public CheckBox checkBox;
        public Long emailUID;
        public int id;
    }

    /**
     * Sets the Unread image based on unread bool value
     * @param viewHolder The viewholder we are toggling
     * @param unread the unread bool value
     */
    public void setUnread(ViewHolder viewHolder, boolean unread){
        if(unread){
            viewHolder.unreadImage.setBackgroundColor(ContextCompat.getColor(context, R.color.unreadBlue));
        }else{
            viewHolder.unreadImage.setBackgroundColor(0);
        }
    }

    public void setShowCheckboxes(){
        if(showCheckboxes){
            showCheckboxes = false;
        }else{
            showCheckboxes = true;
        }
    }

    // Toggles all checkboxes in adapter
    public void toggleAllCheckboxes(Boolean checked){
        if(checked) {
            for (int i = 0; i < emails.size(); i++) {
                selectedPos.add(i);
                emailUIDs.add(emails.get(i).getUID());
            }
        }else{
            for (int i = 0; i < emails.size(); i++) {
                selectedPos.remove((Object)i);
                emailUIDs.remove(emails.get(i).getUID());
            }
        }

    }

    /**
     * Inflates inbox_email.xml and goes through data and adds to view
     * @see Inbox
     * @param position position of item with the data set
     * @param currentView the view to recycle
     * @param viewGroup the parent view
     * @return
     */
    public View getView(final int position, View currentView, ViewGroup viewGroup){
        final ViewHolder viewHolder;
        // New view
        if(currentView == null) {
            // Init new view holder
            viewHolder = new ViewHolder();
            // Inflate view
            currentView = inflater.inflate(R.layout.inbox_email, viewGroup, false);

            // Add to views
            viewHolder.fromText = (TextView) currentView.findViewById(R.id.from_view);
            viewHolder.dateText = (TextView) currentView.findViewById(R.id.date_view);
            viewHolder.subjectText = (TextView) currentView.findViewById(R.id.subject_view);
            viewHolder.unreadImage = (ImageView) currentView.findViewById(R.id.unread_view);
            viewHolder.checkBox = (CheckBox) currentView.findViewById(R.id.email_checkBox);


            currentView.setTag(viewHolder);
        }else {
            viewHolder = (ViewHolder) currentView.getTag();
        }
        // Set data
        viewHolder.fromText.setText(emails.get(position).getFrom().toString());
        viewHolder.dateText.setText(emails.get(position).getReceivedDate().toString());
        viewHolder.subjectText.setText(emails.get(position).getSubject());
        viewHolder.unread = (emails.get(position).getUnread());
        viewHolder.emailUID = (emails.get(position).getUID());
        setUnread(viewHolder, viewHolder.unread);

        // Add checkbox listener
        viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.email_checkBox);
                // Store checked checkbos positions
                if(checkBox.isChecked()){
                    selectedPos.add(position);
                    emailUIDs.add(viewHolder.emailUID);
                }else if(!checkBox.isChecked()){
                    selectedPos.remove((Object) position);
                    emailUIDs.remove(viewHolder.emailUID);
                }
            }
        });

        if(showCheckboxes){
            viewHolder.checkBox.setVisibility(CheckBox.VISIBLE);
        }else{
            viewHolder.checkBox.setVisibility(CheckBox.GONE);
        }

        // Check which checkboxes are checked
        if(selectedPos.contains(position)) {
            viewHolder.checkBox.setChecked(true);
        }else{
            viewHolder.checkBox.setChecked(false);
        }

        return currentView;
    }
}
