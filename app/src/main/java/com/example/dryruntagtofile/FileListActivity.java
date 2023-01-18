package com.example.dryruntagtofile;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.Stack;

public class FileListActivity extends AppCompatActivity {

    Stack<String> browsePath = new Stack<>();
    MyAdapter fileBrowser;
    static int FILE_MOVEMENT_RESULT = 31;
    FileUtilPopup fileContextMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_file_list);

        //Find Components
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        TextView noFilesText = findViewById(R.id.nofiles_text);

        // Get Directory items
        // String filepath = getIntent().getStringExtra("path");
        String filepath = FileUtilities.getFileRoot(this).getPath();
        browsePath.push(filepath);
        File root = new File(filepath);
        File[] filesAndFolders = root.listFiles();

        //Set Report Visible if no Directory items are Available
        if(filesAndFolders==null || filesAndFolders.length == 0){
            noFilesText.setVisibility(View.VISIBLE);
            return;
        }
        noFilesText.setVisibility(View.INVISIBLE);

        fileContextMenu = new FileUtilPopup(this, recyclerView, MemoryDB.getInstance(this));
        fileContextMenu.attachUpdateCallback(new Runnable(){
            @Override
            public void run() {
                Log.d("test_refresh", "Refresh called");
            }
        });

        fileBrowser = new MyAdapter(getApplicationContext(), root, browsePath, fileContextMenu);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(fileBrowser);
    }

    /*@Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String[] filePaths = savedInstanceState.getStringArray("last_path");
        for(int i = 0; i < filePaths.length; i++){
            browsePath.push(filePaths[i]);
        }
        fileBrowser.updateList(new File(browsePath.peek()));
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        String[] filePaths = browsePath.toArray(new String[0]);
        savedInstanceState.putStringArray("last_path", filePaths);
        browsePath.clear();
    }*/

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_MOVEMENT_RESULT) {
            if (resultCode == RESULT_OK) {
                String destinationPath = data.getStringExtra("destination_path");
                Log.d("destination_file", destinationPath);
                fileContextMenu.completeAction(destinationPath, false);
            }
        }
    }

    @Override
    public void onBackPressed() {
        browsePath.pop();
        if(!browsePath.isEmpty()){
            fileBrowser.updateList(new File(browsePath.peek()));
        } else {
            this.finish();
        }
    }
}