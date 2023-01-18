package com.example.dryruntagtofile;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.lights.LightState;
import android.util.Log;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

public class DiskDB extends SQLiteOpenHelper {

    private static final String databaseName = "test_ftr";
    private static final int databaseVersion = 5;
    private static String tableTags = Schema.tableTags;
    private static String tableTagToFiles = Schema.tableTagToFiles;
    private static String tableFilesToTag = Schema.tableFileToTags;
    private static String tableOps = Schema.tableOps;
    private final String delimeter = ";"; // change in ops log too

    private SQLiteDatabase db = null;

    public DiskDB(Context context){
        super(context, databaseName, null, databaseVersion);
        this.db = this.getWritableDatabase();
    }

    public HashMap<String, Integer> getTags(){
        Cursor c = db.query(tableTags, new String[]{Schema.Tags.tagName, Schema.Tags.tag_uid}, null, null, null, null, null);
        HashMap<String, Integer> tags = new HashMap<>(c.getCount());
        c.moveToFirst();
        for(int i = 0; i < c.getCount(); i++){
            tags.put(c.getString(0), c.getInt(1));
            c.moveToNext();
        }
        return tags;
    }

    public int addTag(String tagName){
        ContentValues row = new ContentValues();
        row.put(Schema.Tags.tagName, tagName);
        db.insert(tableTags, null, row);
        Cursor c = db.query(tableTags, new String[]{Schema.Tags.tag_uid}, Schema.Tags.tagName+"=?", new String[]{tagName}, null, null, null);
        c.moveToFirst();
        int tagId = c.getInt(0);
        c.close();
        row.remove(Schema.Tags.tagName);
        row.put(Schema.TagToFiles.tag_uid, tagId);
        row.put(Schema.TagToFiles.fileList, "");
        db.insert(tableTagToFiles, null, row);
        return tagId;
    }

    public void removeTag(int tagId) throws Exception {
        db.beginTransaction();
        String tagUID = String.valueOf(tagId);
        try{
            db.delete(tableTags, Schema.Tags.tag_uid+"=?", new String[]{tagUID});
            db.delete(tableTagToFiles, Schema.TagToFiles.tag_uid+"=?", new String[]{tagUID});
            db.setTransactionSuccessful();
            db.endTransaction();
        } catch (Exception e){
            db.endTransaction();
            e.printStackTrace();
            throw new Exception("Action Failed : failed to delete tag "+tagId);
        }
    }

    public void updateTag(String oldName, String newName){
        ContentValues row = new ContentValues();
        row.put(Schema.Tags.tagName, newName);
        db.update(tableTags, row, Schema.Tags.tagName+"=?", new String[]{oldName});
    }

    public HashMap<String, LinkedHashSet<Integer>> getTaggedFiles(){
        Cursor c = db.query(tableFilesToTag, new String[]{Schema.FileToTags.filePath, Schema.FileToTags.tags}, null, null, null, null, null);
        HashMap<String, LinkedHashSet<Integer>> taggedFiles = new HashMap<>(c.getCount());
        c.moveToFirst();
        for(int i = 0; i < c.getCount(); i++){
            LinkedHashSet<Integer> tagSet = new LinkedHashSet<>();
            String tagList[] = c.getString(1).split(",");
            for(int j = 0; j < tagList.length; j++){
                tagSet.add(Integer.parseInt(tagList[j]));
            }
            taggedFiles.put(c.getString(0), tagSet);
            c.moveToNext();
        }
        return taggedFiles;
    }

    public void attachTag(String filePath, int tagId, boolean exists) throws Exception{
        // check if file exists
        if(!Paths.get(filePath).toFile().exists()){
            throw new Exception("Action Failed : File selected doesn't exist");
        }
        // add to database
        filePath = filePath.replace("'", "\'");
        db.beginTransaction();
        try{
            // update TagToFiles table
            String q = "UPDATE "+tableTagToFiles+" SET "+Schema.TagToFiles.fileList+"='"+filePath+delimeter+"'||"+Schema.TagToFiles.fileList+" WHERE "+Schema.TagToFiles.tag_uid+"="+tagId;
            Log.d("test_run_sql", q);
            db.execSQL(q);
            // update FileToTags table
            if(exists){
                q = "UPDATE "+tableFilesToTag+" SET "+Schema.FileToTags.tags+"='"+tagId+",'||"+Schema.FileToTags.tags+" WHERE "+Schema.FileToTags.filePath+"='"+filePath+"'";
            } else {
                q = "INSERT INTO "+tableFilesToTag+" VALUES(\""+filePath+"\", "+tagId+")";
            }
            Log.d("test_run_sql", q);
            db.execSQL(q);
            db.setTransactionSuccessful();
            db.endTransaction();
        } catch (Exception e){
            db.endTransaction();
            e.printStackTrace();
            throw new Exception("Action Failed : Failed to attach tag");
        }
    }

    private String removeFromTagList(int tagId, String[] tagList){
        ArrayList<String> list = new ArrayList<>(Arrays.asList(tagList));
        list.remove(String.valueOf(tagId));
        String updatedTagList = list.toString().replaceAll("\\s", "");
        updatedTagList = updatedTagList.substring(1, updatedTagList.length()-1);
        return updatedTagList;
    }


    public void detachTag(int tagId, String filePath) throws Exception {
        db.beginTransaction();
        ContentValues updatedRow = new ContentValues();
        try {
            // remove filepath and update database
            Cursor c = db.query(tableTagToFiles, new String[]{Schema.TagToFiles.fileList}, Schema.TagToFiles.tag_uid+"="+tagId, null, null, null, null);
            c.moveToFirst();
            /*String pathList[] = c.getString(0).split(delimeter);
            for(String path : pathList){}*/
            String updatedFileList = c.getString(0).replace(filePath+delimeter, "");
            Log.d("test_run_detach", updatedFileList);
            c.close();
            updatedRow.put(Schema.TagToFiles.fileList, updatedFileList);
            db.update(tableTagToFiles,updatedRow, Schema.TagToFiles.tag_uid+"="+tagId, null);
            updatedRow.remove(Schema.TagToFiles.fileList);
            // remove tagId and update database
            c = db.query(tableFilesToTag, new String[]{Schema.FileToTags.tags}, Schema.FileToTags.filePath+"=?", new String[]{filePath}, null, null, null);
            c.moveToFirst();
            c.moveToFirst();
            String[] oldTagList = c.getString(0).split(",");
            if(oldTagList.length > 1){
                String updatedTagList = removeFromTagList(tagId, oldTagList);
                updatedRow.put(Schema.FileToTags.tags, updatedTagList);
                db.update(tableFilesToTag, updatedRow, Schema.FileToTags.filePath+"=?", new String[]{filePath});
            } else {
                db.delete(tableFilesToTag, Schema.FileToTags.filePath+"=?", new String[]{filePath});
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        } catch (Exception e){
            db.endTransaction();
            e.printStackTrace();
            throw new Exception("Action Failed : Failed to remove tag from the file");
        }
    }

    public FileList listFilesFor(int tagId, Integer limit, Integer offset){
        String l = "";
        if(limit != null && limit != 0){
            l = limit.toString();
            if(offset != null && offset != 0){
                l = offset+","+l;
            }
        }
        Cursor c = db.query(tableTagToFiles, new String[]{Schema.TagToFiles.fileList}, Schema.TagToFiles.tag_uid+"="+tagId, null, null, null, null, l);
        c.moveToFirst();
        // use FileList class to put to array
        String filePaths[] = (c.getString(0).split(delimeter));
        FileItem[] files = new FileItem[filePaths.length];
        for(int i = 0; i < filePaths.length; i++){
            files[i] = new FileItem(filePaths[i]);
        }
        c.close();
        return new FileList(files);
    }

    public String[] getFilePathsFor(int tagId){
        try {
            Cursor c = db.query(tableTagToFiles, new String[]{Schema.TagToFiles.fileList}, Schema.TagToFiles.tag_uid+"="+tagId, null, null, null, null);
            c.moveToFirst();
            String filePaths[] = c.getString(0).split(delimeter);
            c.close();
            Log.d("TagFiles", Arrays.toString(filePaths));
            return filePaths;
        } catch (Exception e){
            Log.e("TagFiles", e.toString());
        }
        return new String[]{};
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String q1 = "CREATE TABLE IF NOT EXISTS "+Schema.tagToFiles,
                q2 = "CREATE TABLE IF NOT EXISTS "+Schema.fileToTags,
                q3 = "CREATE TABLE IF NOT EXISTS "+Schema.tagList,
                q4 = "CREATE TABLE IF NOT EXISTS "+Schema.opsLog;
        db.beginTransaction();
        try{
            db.execSQL(q1);
            db.execSQL(q2);
            db.execSQL(q3);
            db.execSQL(q4);
            db.setTransactionSuccessful();
        } catch (Exception e){
            // something to do if transaction fails
            Log.e("test_run_db", e.toString());
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

        return;
    }
}
