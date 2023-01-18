package com.example.dryruntagtofile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;

public class FilterResult extends AppCompatActivity {

    HashSet<String> filePaths;

    FilterResultAdapter filterResultAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter_result);

        RecyclerView recyclerView = findViewById(R.id.filter_result_RV);
        TextView noFilesText = findViewById(R.id.no_result_files_text);


//        noFilterFileResult = findViewById(R.id.no_filter_file_result);

        filePaths = new HashSet<>(getIntent().getStringArrayListExtra("filePaths"));

        Log.d("TAG_FILE_PATH", filePaths.toString());
        if(filePaths.isEmpty()){
            noFilesText.setVisibility(View.VISIBLE);
            return;
        }
        noFilesText.setVisibility(View.GONE);

        filterResultAdapter = new FilterResultAdapter(this, new ArrayList<>(filePaths));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(filterResultAdapter);

    }
}

class FilterResultAdapter extends RecyclerView.Adapter<FilterResultAdapter.ViewHolder> {

    Context ctx;
    ArrayList<File> files = new ArrayList<>();
    MemoryDB memoryDB;
    FileUtilPopup fileContextMenu;


    public FilterResultAdapter(Context ctx, ArrayList<String> filePaths){
        this.ctx = ctx;
        int len = filePaths.size();
        for(int i = 0; i < len; i++){
            files.add(new File(filePaths.get(i)));
        }
        memoryDB = MemoryDB.getInstance(ctx);
    }

    @NonNull
    @Override
    public FilterResultAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(ctx).inflate(R.layout.recycler_item,parent,false);
        return new FilterResultAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File selectedFile = files.get(position);
        holder.populateData(selectedFile);

        holder.imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
        // attach long click event
        // opens a popup to add and remove tags
        // restricting context menu to file items only
        holder.itemView.setLongClickable(true);
        holder.itemView.setOnLongClickListener(view -> {
            holder.itemView.setActivated(true);
            // open basic file util popup
            Log.d("test_f", selectedFile.getAbsolutePath());
            fileContextMenu.openMenu(selectedFile);
            return true;
        });
        holder.itemView.setOnClickListener(v -> {
            //open the file
            try {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                String type = URLConnection.guessContentTypeFromName(selectedFile.getName()); // get the closest guess for file mime type and corresponding apps to open for it
                intent.setDataAndType(Uri.parse(selectedFile.getAbsolutePath()), type);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(intent);
            }catch (Exception e){
                Toast.makeText(ctx.getApplicationContext(),"Cannot open the file",Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
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

            imageView.setImageResource(R.drawable.ic_baseline_insert_drive_file_24);
            tagPopupOpener.setVisibility(View.VISIBLE);
            tagPopupOpener.setOnClickListener(view -> {
                memoryDB.refreshTagList();
                SettingPopup popup = new SettingPopup(ctx, itemView, memoryDB);
                popup.attachItem(ViewHolder.this);
                popup.openPopup(file.getAbsolutePath());
            });

            try{
                tagContainer.removeAllViews();
                String tags[] = memoryDB.getTagsForFile(file.getAbsolutePath());
                for(int i = 0; i < tags.length; i++){
                    TextView tg = new TextView(ctx);
                    tg.setText(tags[i]);
                    tg.setTextSize(14);
                    tg.setPadding(10, 5, 10, 5);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    params.setMargins(10,2,10,0);
                    tg.setLayoutParams(params);
                    tg.setTextColor(ctx.getColor(R.color.white));
                    tg.setBackgroundColor(ctx.getColor(R.color.teal_700));
                    tagContainer.addView(tg);
                }
            } catch(Exception e){
                Toast.makeText(ctx, "Exception Return (Populate)", Toast.LENGTH_LONG).show();
                return;
            }
        }
    }
}
