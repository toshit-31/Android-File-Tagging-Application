package com.example.dryruntagtofile;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class OpsLog {

    public static final int TAG_DELETED = 1;
    public static final int DETACH_TAG = 2;

    public static final int FILE_MOVED = 11;
    public static final int FILE_DELETED = 12;
    public static final int FILE_RENAMED = 13;

    class Result {
        boolean success = false;
        String errorMsg = "";
        public Result(boolean success, String errorMsg){
            this.success = success;
            this.errorMsg = errorMsg;
        }
    }

    SQLiteDatabase db = null;
    Context ctx = null;
    DiskDB diskDB = null;
    MemoryDB memDB = null;

    public OpsLog(Context ctx, MemoryDB memDB){
        this.ctx = ctx;
        this.diskDB = new DiskDB(ctx);
        this.db = this.diskDB.getWritableDatabase();
        this.memDB = memDB;
        OpsLog self = this;

        ScheduledExecutorService operationScheduler = Executors.newSingleThreadScheduledExecutor();
        operationScheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {

                Cursor c = self.db.query(Schema.tableOps, new String[]{Schema.OpsLog.op, Schema.OpsLog.data, "id"}, null, null, null, null, null, "1");
                if(c.getCount() == 0){
                    return;
                }
                c.moveToFirst();
                try {
                    JSONObject data = null;
                    data = new JSONObject(c.getString(1));
                    Log.d("schedule_data", data.toString());
                    String taskCompletedMsg = "";
                    boolean completed = false;
                    switch(c.getInt(0)){
                        case DETACH_TAG: {
                            int tagId = data.getInt("tag_id");
                            String filePath = data.getString("old_path");
                            completed = self.detachTag(tagId, filePath);
                            memDB.removeTagFromFile(filePath, memDB.getTagNameById(tagId));
                            break;
                        }
                        case TAG_DELETED:{
                            int tagId = data.getInt("tag_id");
                            completed = self.deleteTagData(tagId);
                            taskCompletedMsg = "Data for the tag cleared";
                            memDB.refreshMemoryState();
                            break;
                        }
                        case FILE_RENAMED:{
                            String old_path = data.getString("old_path");
                            String new_path = data.getString("new_path");
                            completed = self.fileMoved(old_path, new_path, true);
                            memDB.replaceFilePathAFOL(old_path, new_path);
                            break;
                        }
                        case FILE_MOVED:{
                            String old_path = data.getString("old_path");
                            String new_path = data.getString("new_path");
                            completed = self.fileMoved(old_path, new_path, false);
                            memDB.replaceFilePathAFOL(old_path, new_path);
                            break;
                        }
                        case FILE_DELETED: {
                            String old_path = data.getString("old_path");
                            completed = self.fileDeleted(old_path);
                            memDB.deleteFilePathAFOL(old_path);
                            break;
                        }
                    }
                    // if successfully not completed do not delete
                    /*if(!completed) return;*/
                    // proceed if task is successfully completed
                    self.db.delete(Schema.tableOps, "id="+c.getString(2), null);
                    c.close();

                    Intent intent = new Intent("event_received");
                    ctx.sendBroadcast(intent);

                    if(taskCompletedMsg.length() == 0) return;
                    ((Activity)self.ctx).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(self.ctx, "Task Completed", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    Log.e("test_run_ops", e.toString());
                    e.printStackTrace();
                    self.db.delete(Schema.tableOps, "id="+c.getString(2), null);
                    return;
                }
            }
        },0, 200, TimeUnit.MILLISECONDS);
    }

    public Result enqueue(int op, Integer tagId, String old_path, String new_path) {
        ContentValues r = new ContentValues();
        JSONObject data = new JSONObject();
        r.put(Schema.OpsLog.op, op);
        r.put(Schema.OpsLog.completed, false);
        try{
            switch(op){
                case DETACH_TAG: {
                    if(tagId == null || old_path == null) throw new IllegalArgumentException("Argument tagId and old_path required");

                    data.put("tag_id", tagId);
                    data.put("old_path", old_path);

                    r.put(Schema.OpsLog.data, data.toString());
                    break;
                }
                case TAG_DELETED: {
                    if(tagId == null) throw new IllegalArgumentException("Argument tagId required");

                    data.put("tag_id", tagId);

                    r.put(Schema.OpsLog.data, data.toString());
                    break;
                }
                case FILE_RENAMED:
                case FILE_MOVED: {
                    if(old_path == null || new_path == null) throw new IllegalArgumentException("Argument old_path and new_path are required");

                    data.put("old_path", old_path);
                    data.put("new_path", new_path);

                    r.put(Schema.OpsLog.data, data.toString());
                    break;
                }
                case FILE_DELETED: {
                    if(old_path == null) throw new IllegalArgumentException("Argument old_path required");

                    data.put("old_path", old_path);

                    r.put(Schema.OpsLog.data, data.toString());
                    break;
                }
            }
            this.db.insert(Schema.tableOps, null, r);
            return new Result(true, "OK");
        } catch(Exception e){
            return new Result(false, e.getMessage());
        }
    }

    private String removeFromTagList(int tagId, String[] tagList){
        ArrayList<String> list = new ArrayList<>(Arrays.asList(tagList));
        list.remove(String.valueOf(tagId));
        String updatedTagList = list.toString().replaceAll("\\s", "");
        updatedTagList = updatedTagList.substring(1, updatedTagList.length()-1);
        return updatedTagList;
    }

    public boolean detachTag(int tagId, String filePath){
        db.beginTransaction();
        try {
            Cursor c = this.db.query(Schema.tableFileToTags, new String[]{Schema.FileToTags.tags}, Schema.FileToTags.filePath+"=?", new String[]{filePath}, null, null, null);
            c.moveToFirst();
            String[] tagList = c.getString(0).split(",");
            c.close();
            if(tagList.length > 1){
                String updatedTagList = removeFromTagList(tagId, tagList);
                String q = "UPDATE "+Schema.tableFileToTags+" SET "+Schema.FileToTags.tags+"='"+updatedTagList+"' WHERE "+Schema.FileToTags.filePath+"='"+filePath+"'";
                db.execSQL(q);
            } else {
                db.delete(Schema.tableFileToTags, Schema.FileToTags.filePath+"='"+filePath+"'", null);
            }
            db.setTransactionSuccessful();
            db.endTransaction();
            return true;
        } catch(Exception e){
            db.endTransaction();
            return false;
        }
    }

    private Cursor getTaggedFileByPage(int limit, int offset){
        return this.db.query(Schema.tableFileToTags, new String[]{Schema.FileToTags.tags, Schema.FileToTags.filePath}, null, null, null, null, null, offset+","+limit);
    }

    /*public boolean deleteTagData(int tagId){
        db.beginTransaction();
        try {
            int limit = 50;
            Cursor c = getTaggedFileByPage(limit, 0);
            boolean keepFetching = true;
            int p = 0;
            while(keepFetching){
                Log.d("test_run-count", String.valueOf(c.getCount()));
                if(c.getCount() < limit){
                    keepFetching = false;
                }
                if(c.getCount() == 0) return true;
                c.moveToFirst();
                for(int i = 0; i < c.getCount(); i++){
                    String filePath = c.getString(1);
                    String tags = c.getString(0);
                    Log.d("test_run_delete-tag", filePath);
                    String tagList[] = tags.split(",");
                    String updatedTags = removeFromTagList(tagId, tagList);
                    if(!tags.equals(updatedTags)){
                        String q = "UPDATE "+Schema.tableFileToTags+" SET "+Schema.FileToTags.tags+"='"+updatedTags+"' WHERE "+Schema.FileToTags.filePath+"='"+filePath+"'";
                        db.execSQL(q);
                    }
                }
                c = getTaggedFileByPage(limit, ++p*limit);
            }
            c.close();
            db.setTransactionSuccessful();
            db.endTransaction();
            return true;
        } catch(Exception e){
            Log.e("TAG_DELETE", e.toString());
            db.endTransaction();
            return false;
        }
    }*/

    // version 2
    public boolean deleteTagData(int tagId){
        db.beginTransaction();
        try {
            Cursor c = db.query(Schema.tableTagToFiles, new String[]{Schema.TagToFiles.fileList}, Schema.TagToFiles.tag_uid+"="+tagId, null, null, null, null);
            c.moveToFirst();
            String[] filePaths = c.getString(0).split(";"); // change when delimeter changed
            int filePathsN = filePaths.length;
            for(int i = 0; i < filePathsN; i++){
                String path = filePaths[i];
                Log.d("test_run_delete-tag", path);
                // detahc from single file
                diskDB.detachTag(tagId, path);
            }
            // remove tag from database
            diskDB.removeTag(tagId);
            c.close();
            db.setTransactionSuccessful();
            db.endTransaction();
            return true;
        } catch(Exception e){
            Log.e("TAG_DELETE", e.toString());
            db.endTransaction();
            return false;
        }
    }

    public boolean fileMoved(String old_path, String new_path, boolean renameFlag){
        String oldName = Paths.get(old_path).getFileName().toString();
        String newName = Paths.get(new_path).getFileName().toString();
        if(renameFlag){
            if(oldName.equals(newName)) return true;
        } else {
            if(old_path.equals(new_path)) return true;
        }
        db.beginTransaction();
        ContentValues values = new ContentValues();
        try {
            Cursor c = db.query(Schema.tableFileToTags, new String[]{Schema.FileToTags.tags}, Schema.FileToTags.filePath+"=?", new String[]{old_path}, null, null, null);
            c.moveToFirst();
            String tagsWithFile[] = c.getString(0).split(",");
            // update in TagToFiles table
            for(int i = 0; i < tagsWithFile.length; i++){
                String whereStr = Schema.TagToFiles.tag_uid+"="+tagsWithFile[i];
                c = db.query(Schema.tableTagToFiles, new String[]{Schema.TagToFiles.fileList}, whereStr, null, null, null, null);
                c.moveToFirst();
                String fileList = c.getString(0);
                String updatedFileList = fileList.replace(old_path, new_path);
                values.put(Schema.TagToFiles.fileList, updatedFileList);
                db.update(Schema.tableTagToFiles, values, whereStr, null);
            }
            // update in FileToTags table
            values.clear();
            values.put(Schema.FileToTags.filePath, new_path);
            db.update(Schema.tableFileToTags, values, Schema.FileToTags.filePath+"=?", new String[]{old_path});

            c.close();
            db.setTransactionSuccessful();
            db.endTransaction();
            return true;
        } catch(Exception e){
            db.endTransaction();
            return false;
        }
    }

    public boolean fileDeleted(String old_path){
        db.beginTransaction();
        ContentValues values = new ContentValues();
        try {
            Cursor c = db.query(Schema.tableFileToTags, new String[]{Schema.FileToTags.tags}, Schema.FileToTags.filePath+"=?", new String[]{old_path}, null, null, null);
            c.moveToFirst();
            String tagsWithFile[] = c.getString(0).split(",");
            // update in TagToFiles table
            for(int i = 0; i < tagsWithFile.length; i++){
                String whereStr = Schema.TagToFiles.tag_uid+"="+tagsWithFile[i];
                c = db.query(Schema.tableTagToFiles, new String[]{Schema.TagToFiles.fileList}, whereStr, null, null, null, null);
                c.moveToFirst();
                String fileList = c.getString(0);
                String updatedFileList = fileList.replace(old_path+";", "");
                values.put(Schema.TagToFiles.fileList, updatedFileList);
                db.update(Schema.tableTagToFiles, values, whereStr, null);
            }
            // update in FileToTags table
            db.delete(Schema.tableFileToTags, Schema.FileToTags.filePath+"=?", new String[]{old_path});

            c.close();
            db.setTransactionSuccessful();
            db.endTransaction();
            return true;
        } catch(Exception e){
            db.endTransaction();
            return false;
        }
    }
}
