package harps.swanuniemailclient;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

/**
 * Created by eghar on 13/04/2017.
 */

public class AttachmentAdapter extends BaseAdapter {
    private Context context;
    private List<File> attachments = new ArrayList<>();

    public AttachmentAdapter(Context context, List<File> attachments){
        this.context = context;
        this.attachments = attachments;
    }

    @Override
    public int getCount() {
        return attachments.size();
    }

    @Override
    public Object getItem(int id) {
        return attachments.get(id);
    }

    @Override
    public long getItemId(int id) {
        return id;
    }


    public View getView(final int id, View currentView, ViewGroup viewGroup){

        Button attachmentButton = new Button(context);
        attachmentButton.setId(id);
        attachmentButton.setText(attachments.get(id).getName());



        attachmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (context instanceof  EmailActivity) {
                    viewAttachment(context, attachments.get(id));
                }
            }
        });
        if(context instanceof SendEmail){
            attachmentButton.setClickable(false);
        }

        return attachmentButton;
    }

    public void viewAttachment(Context context, File url){
        File file = url;
        System.out.println("IN HERE");
        String filename = file.getName();

        String fileType = URLConnection.guessContentTypeFromName(filename);

        if (fileType == null) {
            fileType = "*/*";
        }
        Uri data = Uri.parse("content://harps.swanuniemailclient/" + filename);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(data, fileType);
        try {
            ((Activity)context).startActivity(intent);
        }catch (ActivityNotFoundException e){
            Toast.makeText(context,
                    "You currently don't have an application for the file type: " + fileType + ". Please download one to open it.",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
