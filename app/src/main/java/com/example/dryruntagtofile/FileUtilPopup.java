package com.example.dryruntagtofile;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileUtilPopup {

    Context ctx;
    MemoryDB memdb;
    OpsLog ops;
    View view;
    File file;
    View popupRoot;
    PopupWindow popup;
    boolean actionInit = false;
    String action = "";
    Runnable callback;

    MyAdapter adapter;
    int pos;


    public FileUtilPopup(Context ctx, final View view, MemoryDB memdb){
        this.ctx = ctx;
        this.memdb = memdb;
        this.view = view;
        this.ops = new OpsLog(ctx, memdb);
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(LAYOUT_INFLATER_SERVICE);
        popupRoot = inflater.inflate(R.layout.file_util_popup, null);
        popup = new PopupWindow(popupRoot, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, true);
        popupRoot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popup.dismiss();
            }
        });
    }

    public void attachUpdateCallback(Runnable cb){
        this.callback = cb;
    }

    public void openMenu(File file){
        this.file = file;
        popup.showAtLocation(view, Gravity.CENTER, 0, 0);

        popupRoot.findViewById(R.id.ma_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popup.dismiss();
                actionInit = true;
                action = "copy";
                Intent intent = new Intent(ctx, FolderSelection.class);
                intent.putExtra("context_file_path", file.getAbsolutePath());
                ActivityCompat.startActivityForResult((Activity) ctx, intent, FileListActivity.FILE_MOVEMENT_RESULT, null);
            }
        });
        popupRoot.findViewById(R.id.ma_move).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popup.dismiss();
                actionInit = true;
                action = "move";
                Intent intent = new Intent(ctx, FolderSelection.class);
                intent.putExtra("context_file_path", file.getAbsolutePath());
                ActivityCompat.startActivityForResult((Activity) ctx, intent, FileListActivity.FILE_MOVEMENT_RESULT, null);
            }
        });

        popupRoot.findViewById(R.id.ma_del).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popup.dismiss();

                AlertDialog.Builder delPopup = new AlertDialog.Builder(ctx);
                delPopup.setTitle("Delete");
                delPopup.setMessage("Are you sure you want to delete "+file.getName()+" ?");

                delPopup.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                delPopup.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        FileUtilities.deleteFile(Paths.get(file.getAbsolutePath()));
                        ops.enqueue(OpsLog.FILE_DELETED, null, file.getAbsolutePath(), null);
                        ((Activity) ctx).runOnUiThread(callback);
                    }
                });
                delPopup.show();
            }
        });

        popupRoot.findViewById(R.id.ma_rename).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popup.dismiss();
                LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(LAYOUT_INFLATER_SERVICE);
                LinearLayout v = (LinearLayout) inflater.inflate(R.layout.rename_popup, null);

                EditText fileNameView = ((EditText) v.findViewById(R.id.text_input));
                TextView fileTypeView = ((TextView) v.findViewById(R.id.file_type_info));
                String fileName;
                String fileType = "";
                if(file.isDirectory()){
                    fileTypeView.setVisibility(View.GONE);
                    fileNameView.setText(file.getName());
                } else {
                    fileName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    fileType = file.getName().substring(file.getName().lastIndexOf('.'));
                    fileNameView.setText(fileName);
                    fileTypeView.setText(fileType);
                    fileTypeView.setVisibility(View.VISIBLE);
                }

                AlertDialog.Builder renamePopup = new AlertDialog.Builder(ctx);
                renamePopup.setTitle("Enter the new name");
                renamePopup.setView(v);

                renamePopup.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                final String finalFileType = fileType;
                renamePopup.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        String fileNewName = fileNameView.getText().toString();
                        int res = FileUtilities.renameFileOrFolder(Paths.get(file.getAbsolutePath()), fileNewName);
                        if((res & FileUtilities.FAILED) == FileUtilities.FAILED){
                            Toast.makeText(ctx, "Renaming file failed", Toast.LENGTH_SHORT).show();
                        } else {
                            if(file.isDirectory()){
                                fileNewName = Paths.get(file.getParent(), fileNewName).toString();
                            } else {
                                fileNewName = Paths.get(file.getParent(), fileNewName+finalFileType).toString();
                            }
                            Log.d("test_replace", fileNewName);
                            // memdb.replaceFilePath(file.getAbsolutePath(), fileNewName);
                            ops.enqueue(OpsLog.FILE_RENAMED, null, file.getAbsolutePath(), fileNewName);
                            ((Activity) ctx).runOnUiThread(callback);
                        }
                    }
                });
                renamePopup.show();
            }
        });
    }

    public boolean completeAction(String destinationFilePath, boolean replace){
        if(!actionInit) return false;
        Path src = Paths.get(this.file.getAbsolutePath());
        Path dest = Paths.get(destinationFilePath);
        int res;
        try {
            switch(action){
                case "copy": {
                    res = FileUtilities.copyFileOrFolder(src, dest);
                    break;
                }
                case "move": {
                    res = FileUtilities.moveFile(src, dest, false, false);
                    String destFilePath = Paths.get(destinationFilePath, src.getFileName().toString()).toString();
                    ops.enqueue(OpsLog.FILE_MOVED, null, file.getAbsolutePath(), destFilePath);
                    break;
                }
                default: {
                    res = FileUtilities.FAILED;
                }
            }
            action = "";
            if(res != FileUtilities.SUCCESS){
                return false;
            }
            ((Activity) ctx).runOnUiThread(callback);
            return true;
        } catch(Exception e){
            return false;
        }
    }
}
