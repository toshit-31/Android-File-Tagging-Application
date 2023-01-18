package com.example.dryruntagtofile;

import static android.view.View.GONE;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Stack;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>{

    Context context;
    ArrayList<File> filesAndFolders = new ArrayList<>();
    public MemoryDB memdb;
    Stack<String> browsePath;
    FileUtilPopup fileContextMenu;
    BroadcastReceiver br;

    public MyAdapter(Context context, File root, Stack<String> browsePath, FileUtilPopup fileContextMenu){ //Constructor (set from FileListActivity)
        this.context = context;
        this.updateList(root);
        this.memdb = MemoryDB.getInstance(context);
        this.browsePath = browsePath;
        this.fileContextMenu = fileContextMenu;
        this.br = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals("event_received")){
                    Log.d("event_received", intent.getAction());
                    //memdb.refreshMemoryState();
                    if(!browsePath.isEmpty()) {
                        MyAdapter.this.updateList(new File(browsePath.peek()));
                    }
                }
            }
        };
        context.registerReceiver(this.br, new IntentFilter("event_received"));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recycler_item,parent,false);
        /*view.setClickable(true);
        view.setFocusable(true);*/
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        File selectedFile = filesAndFolders.get(position);
        holder.populateData(selectedFile);

        if(selectedFile.isDirectory()){
            holder.imageView.setImageResource(R.drawable.ic_baseline_folder_24);
        }else{
            holder.imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
            // attach long click event
            // opens a popup to add and remove tags
            // restricting context menu to file items only
            holder.itemView.setLongClickable(true);
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    holder.itemView.setActivated(true);
                    // open basic file util popup
                    Log.d("test_f", selectedFile.getAbsolutePath());
                    fileContextMenu.openMenu(selectedFile);
                    return true;
                }
            });
        }


        //if directory - open file list recursively
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedFile.isDirectory()){
                    String path = selectedFile.getAbsolutePath();
                    browsePath.push(path);
                    updateList(selectedFile);
                }else{
                    //open the file
                    try {
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        String type = URLConnection.guessContentTypeFromName(selectedFile.getName()); // get the closest guess for file mime type and corresponding apps to open for it
                        intent.setDataAndType(FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", selectedFile), type);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        context.startActivity(intent);
                    }catch (Exception e){
                        Log.d("FILE_OPEN", e.toString());
                        Toast.makeText(context.getApplicationContext(),"Cannot open the file",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return filesAndFolders.size();
    }

    public boolean updateList(File dir){
        Log.d("event_received", "update list called");
        if(dir.isDirectory()){
            filesAndFolders.clear();
            this.notifyDataSetChanged();
            filesAndFolders.addAll(Arrays.asList(dir.listFiles()));
            filesAndFolders.sort(new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    int dir1 = file1.isFile() ? 1 : 0;
                    int dir2 = file2.isFile() ? 1: 0;
                    return dir1-dir2;
                }
            });
            this.notifyDataSetChanged();
            return true;
        }
        return false;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView textView;
        ImageView imageView;
        LinearLayout tagContainer;
        ImageButton tagPopupOpener;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.file_name_text_view);
            imageView = itemView.findViewById(R.id.icon_view);
            tagContainer = itemView.findViewById(R.id.tag_cont);
            tagPopupOpener = itemView.findViewById(R.id.three_dot_btn);
        }

        public void populateData(File file){
            textView.setText(file.getName());
            if(file.isDirectory()){
                imageView.setImageResource(R.drawable.ic_baseline_folder_24);
                tagPopupOpener.setVisibility(View.GONE);
            }else{
                imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
                tagPopupOpener.setVisibility(View.VISIBLE);
                tagPopupOpener.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        memdb.refreshTagList();
                        SettingPopup popup = new SettingPopup(context, itemView, memdb);
                        popup.attachItem(ViewHolder.this);
                        popup.openPopup(file.getAbsolutePath());
                    }
                });
            }

            try{
                tagContainer.removeAllViews();
                String tags[] = memdb.getTagsForFile(file.getAbsolutePath());
                for(int i = 0; i < tags.length; i++){
                    TextView tg = new TextView(context);
                    tg.setText(tags[i]);
                    tg.setTextSize(14);
                    tg.setPadding(10, 5, 10, 5);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(10,2,10,0);
                    tg.setLayoutParams(params);
                    tg.setTextColor(context.getColor(R.color.white));
                    tg.setBackgroundColor(context.getColor(R.color.teal_700));
                    tagContainer.addView(tg);
                }
            } catch(Exception e){
                return;
            }
        }
    }
}
