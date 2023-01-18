package com.example.dryruntagtofile;

import static android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.Toast;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.security.Permission;
import java.util.Arrays;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Find Components
        MaterialButton filesBtn = findViewById(R.id.filesButton);
        MaterialButton tagsBtn = findViewById(R.id.tagsButton);

        /*try {
            startForegroundService(new Intent(getApplicationContext(), FileSystemService.class));
        } catch (Exception e){
            Log.e("Initialising Error", e.toString());
        }*/

        Context self = this;
        //Click on FILES button
        filesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermission()){
                    Intent intent = new Intent(MainActivity.this, FileListActivity.class);
                    String path = FileUtilities.getFileRoot(self).getPath();
                    intent.putExtra("path",path);
                    startActivity(intent);
                }else{ //permission denied
                    requestPermission();
                }
            }
        });

        //Click on TAGS button
        tagsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkPermission()){
                    Intent intent = new Intent(MainActivity.this, TagListViewingGrid.class);
                    startActivity(intent);
                }else{ //permission denied
                    requestPermission();
                }
            }
        });
    }

    private boolean checkPermission(){
        return Environment.isExternalStorageManager();
    }

    private void requestPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)){
            Toast.makeText(MainActivity.this, "Storage permissions required", Toast.LENGTH_LONG).show();
            return ;
        }
        Intent intent = new Intent(ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);

        this.startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 31) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission GRANTED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_SHORT).show();
            }
        }
    }
}