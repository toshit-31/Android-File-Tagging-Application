package com.example.dryruntagtofile;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Stack;

public class FolderSelection extends AppCompatActivity {

    ArrayList<String> folders = new ArrayList<>();
    Stack<String> browsePath = new Stack<String>();
    File currentFolder;
    ArrayAdapter<String> folderList;
    String action;
    File contextFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_selection);
        Intent intent = getIntent();

        action = intent.getStringExtra("action");
        String contextFilePath = intent.getStringExtra("context_file_path");
        contextFile = new File(contextFilePath);

        folderList = new ArrayAdapter<>(getApplicationContext(), R.layout.recycler_item, R.id.file_name_text_view);
        ListView folderListView = (ListView) findViewById(R.id.folder_list);

        File root = FileUtilities.getFileRoot(this);
        currentFolder = root;
        browsePath.push(currentFolder.getAbsolutePath());
        openFolder(root);

        String actionInfoText = "Moving : "+contextFile.getName()+" \n from : "+contextFile.getParent();
        ((TextView) findViewById(R.id.action_info)).setText(actionInfoText);

        findViewById(R.id.action_comp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                returnResult();
            }
        });

        folderListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                File nextFolder = new File(currentFolder.getAbsolutePath()+"/"+folderList.getItem(i));
                Log.d("test", nextFolder.getAbsolutePath());
                if(nextFolder.exists() && nextFolder.isDirectory()){
                    browsePath.push(currentFolder.getAbsolutePath());
                    openFolder(nextFolder);
                } else {
                    folderList.remove(folderList.getItem(i));
                }
            }
        });
        folderListView.setAdapter(folderList);
    }

    public void returnResult(){
        Intent intent = new Intent();
        intent.putExtra("destination_path", currentFolder.getAbsolutePath());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void openFolder(File folder){
        currentFolder = folder;
        folderList.clear();
        File[] childFolders = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        Log.d("test_files", Arrays.toString(childFolders));
        Log.d("test_files", currentFolder.getAbsolutePath());
        for(int i = 0; i< childFolders.length; i++){
            folderList.add(childFolders[i].getName());
        }
        folderList.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        if(browsePath.isEmpty()) {
            super.onBackPressed();
        } else {
            browsePath.pop();
            this.openFolder(new File(browsePath.peek()));
        }
    }
}