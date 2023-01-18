package com.example.dryruntagtofile;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Iterator;

import kotlin.jvm.Synchronized;

public class MemoryDB {
    private static MemoryDB instance = null;
    private HashMap<String, LinkedHashSet<Integer>> taggedFiles = null;
    private HashMap<String, Integer> tags = null;
    private HashMap<Integer, String> tagIds = null;
    DiskDB diskDB = null;
    OpsLog ops = null;

    MemoryDB(Context ctx){
        diskDB = new DiskDB(ctx);
        ops = new OpsLog(ctx, this);
        refreshMemoryState();
    }

    public void refreshTagList(){
        tags = diskDB.getTags();
        // mapping tagid -> tag
        tagIds = new HashMap<>(tags.size());
        String[] keys = tags.keySet().toArray(new String[0]);
        for (String key : keys) {
            tagIds.put(tags.get(key), key);
        }
    }

    public void refreshMemoryState(){
        taggedFiles = diskDB.getTaggedFiles();
        refreshTagList();
    }

    @Synchronized
    public static synchronized MemoryDB getInstance(Context context){
        synchronized (new Object()){
            if(instance == null){
                instance = new MemoryDB(context);
            }
            return instance;
        }
    }

    public String getTagNameById(int tagId){
        return tagIds.getOrDefault(tagId, "");
    }

    public int getTagIdByName(String tagName) {return tags.getOrDefault(tagName, -1); }

    public ArrayList<String> getTags(){
        return new ArrayList(tags.keySet());
    }

    public HashSet<String> getTagsSet(){
        return new HashSet<>(tags.keySet());
    }

    public HashSet<Integer> getUIDSet(HashSet<String> tagNames){
        HashSet<Integer> UIDSet = new HashSet<>();
        int id;
        for(String tagname: tagNames){
            try{
                id = tags.get(tagname);
                UIDSet.add(id);
            }
            catch(NullPointerException e) { continue; }
        }
        return UIDSet;
    }

    public void addTag(String tagName) throws Exception{
        int id = diskDB.addTag(tagName);
        if(tags.containsKey(tagName)){
            throw new Exception("Action Failed : Tag already present");
        }
        tags.put(tagName, id);
        tagIds.put(id, tagName);
    }

    public void removeTag(String tagName) throws Exception{
        int id = tags.get(tagName);

        tags.remove(tagName);
        tagIds.remove(id);

        ops.enqueue(OpsLog.TAG_DELETED, id, null, null);
    }

    public void updateTag(String oldName, String newName){
        diskDB.updateTag(oldName, newName);

        int id = tags.get(oldName);
        tags.remove(oldName);
        tags.put(newName, id);
        tagIds.put(id, newName);
    }

    public String[] removeTagFromFile(String filePath, String tagName) throws Exception {
        int tagId = tags.getOrDefault(tagName, -1);
        if(tagId == -1 || !taggedFiles.containsKey(filePath)){
            throw new Exception("Action Failed : No such tag or file exists");
        }
        diskDB.detachTag(tagId, filePath);
        taggedFiles.get(filePath).remove(tagId);
        if(taggedFiles.get(filePath).isEmpty()) taggedFiles.remove(filePath);
        return getTagsForFile(filePath);
    }

    public String[] addTagToFile(String filePath, String tagName) throws Exception{
        int id = tags.getOrDefault(tagName, -1);
        if(id == -1){
            throw new Exception("Action Failed : No such tag exists");
        }
        if(taggedFiles.containsKey(filePath)){
            if(taggedFiles.get(filePath).contains(id)) {
                return getTagsForFile(filePath);
            };
            LinkedHashSet<Integer> fileTags = new LinkedHashSet<>();
            fileTags.add(id);
            fileTags.addAll(taggedFiles.get(filePath));
            taggedFiles.put(filePath, fileTags);
            diskDB.attachTag(filePath, id, true);
        } else {
            LinkedHashSet<Integer> fileTags = new LinkedHashSet<>();
            fileTags.add(id);
            taggedFiles.put(filePath, fileTags);
            diskDB.attachTag(filePath, id, false);
        }
        return getTagsForFile(filePath);
    }

    public String[] getTagsForFile(String filePath) throws Exception{
        LinkedHashSet<Integer> tagSet = taggedFiles.get(filePath);
        if(tagSet == null){
            throw new Exception("Action Failed : No such file exists");
        }
        ArrayList<String> fileTags = new ArrayList<>();
        Iterator<Integer> it = tagSet.iterator();
        for(int i = 0; it.hasNext(); i++){
            int tagId = it.next();
            if(tagIds.containsKey(tagId)){
                fileTags.add(i, tagIds.get(tagId));
            }
            else {
                tagSet.remove(tagId);
                ops.enqueue(OpsLog.DETACH_TAG, tagId, filePath, null);
            }
        }
        taggedFiles.put(filePath, tagSet);
        return fileTags.toArray(new String[0]);
    }

    public void replaceFilePathAFOL(String oldFilePath, String newFilePath){
        LinkedHashSet<Integer> tagSet = taggedFiles.get(oldFilePath);
        taggedFiles.remove(oldFilePath);
        taggedFiles.put(newFilePath, tagSet);
    }

    public void deleteFilePathAFOL(String filePath){
        taggedFiles.remove(filePath);
    }
}