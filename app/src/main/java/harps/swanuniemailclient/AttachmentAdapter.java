package harps.swanuniemailclient;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * @file AttachmentsAdapter.java
 * @author Ed Harper
 * @date 13/04/2017
 *
 * The attachments grid view adapter, adds attachments buttons to view
 */

public class AttachmentAdapter extends BaseAdapter {

    // File final variables
    private final String URI_CONTENT = "content://harps.swanuniemailclient/";
    private final String FILE_TYPE = "*/*";

    // Error final strings
    private final String URI_ERROR_P1 = "You currently don't have an application for the file type: ";
    private final String URI_ERROR_P2 = ". Please download one to open it.";

    private Context context;
    private List<File> attachments = new ArrayList<>();

    /**
     * Initialises attachment adapter
     * @param context passed context
     * @param attachments the attachments to display
     */
    public AttachmentAdapter(Context context, List<File> attachments){
        this.context = context;
        this.attachments = attachments;
    }

    /**
     * Gets total attachments
     * @return the total attachments
     */
    @Override
    public int getCount() {
        return attachments.size();
    }

    /**
     * Gets the attachment at the passed position
     * @param position the position of the attachment
     * @return
     */
    @Override
    public Object getItem(int position) {
        return attachments.get(position);
    }

    /**
     * Gets the attachment position in the gridview
     * @param position the position in grid view
     * @return
     */
    @Override
    public long getItemId(int position) {
        return position;
    }


    /**
     * Adds attachments buttons to view
     * @param position the items position
     * @param currentView the view to be recycled
     * @param viewGroup the parent view
     * @return recycled currentView
     */
    public View getView(final int position, View currentView, ViewGroup viewGroup){

        Button attachmentButton = new Button(context);
        attachmentButton.setId(position);
        attachmentButton.setText(attachments.get(position).getName());

        attachmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof  EmailActivity) {
                    viewAttachment(context, attachments.get(position));
                }
            }
        });
        if(context instanceof SendEmailActivity){
            attachmentButton.setClickable(false);
        }

        return attachmentButton;
    }

    public void viewAttachment(Context context, File url){
        File file = url;
        String filename = file.getName();

        String fileType = URLConnection.guessContentTypeFromName(filename);

        if (fileType == null) {
            fileType = FILE_TYPE;
        }
        System.out.println(fileType);
        Uri data = Uri.parse( URI_CONTENT + filename);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(data, fileType);
        try {
            ((Activity)context).startActivity(intent);
        }catch (ActivityNotFoundException e){
            Toast.makeText(context,
                    URI_ERROR_P1 + fileType + URI_ERROR_P2,
                    Toast.LENGTH_SHORT).show();
        }
    }
}
