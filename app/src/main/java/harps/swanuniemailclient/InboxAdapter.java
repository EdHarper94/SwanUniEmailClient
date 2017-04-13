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
 * Created by eghar on 31/03/2017.
 */

public class InboxAdapter extends BaseAdapter{

    Context context;
    ArrayList<ReceivedEmail> emails = new ArrayList<>();
    LayoutInflater inflater;
    List<Integer> selectedPos;
    List<Long> emailUIDs;
    Boolean showCheckboxes;

    public InboxAdapter(Context context, ArrayList<ReceivedEmail> emails){
        this.context = context;
        this.emails = emails;
        this.inflater = (LayoutInflater.from(context));
        this.selectedPos = new ArrayList<>();
        this.emailUIDs = new ArrayList<>();
        this.showCheckboxes = false;
    }


    public int getCount(){
        return emails.size();
    }

    public Object getItem(int id){
        System.out.println("THIS IS THE ERROR");
        return emails.get(id);
    }

    public long getItemId(int id){
        System.out.println("THIS IS THE ERROR 2");
        return id;
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
    }

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

    /**
     * Inflates inbox_email.xml and goes through data and adds to view
     * @see Inbox
     * @param id
     * @param currentView
     * @param viewGroup
     * @return
     */
    public View getView(final int id, View currentView, ViewGroup viewGroup){
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
        viewHolder.fromText.setText(emails.get(id).getFrom().toString());
        viewHolder.dateText.setText(emails.get(id).getReceivedDate().toString());
        viewHolder.subjectText.setText(emails.get(id).getSubject());
        viewHolder.unread = (emails.get(id).getUnread());
        viewHolder.emailUID = (emails.get(id).getUID());
        setUnread(viewHolder, viewHolder.unread);

        // Add checkbox listener
        viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CheckBox checkBox = (CheckBox) view.findViewById(R.id.email_checkBox);
                // Store checked checkbos positions
                if(checkBox.isChecked()){
                    selectedPos.add(id);
                    emailUIDs.add(viewHolder.emailUID);
                    System.out.println(viewHolder.emailUID);
                }else if(!checkBox.isChecked()){
                    selectedPos.remove((Object) id);
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
        if(selectedPos.contains(id)) {
            viewHolder.checkBox.setChecked(true);
        }else{
            viewHolder.checkBox.setChecked(false);
        }

        return currentView;
    }
}
