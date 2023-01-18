package com.example.dryruntagtofile;

import android.content.Context;
import android.util.Log;

import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.util.LinkedList;
import java.util.Queue;

public class FileMovementLog {

    private static class Movement{
        public String event = null;
        public String parentDir = null;
        public String fileName = null;

        public Movement(WatchEvent<?> event, String parentDir, String fileName){
            this.event = event.kind().toString();
            this.parentDir = parentDir;
            this.fileName = fileName;
        }

        public String absolutePath(){
            return Paths.get(this.parentDir, this.fileName).toAbsolutePath().toString();
        }

        @Override
        public String toString() {
            return "Movement{" +
                    "event='" + event + '\'' +
                    ", parentDir='" + parentDir + '\'' +
                    ", fileName='" + fileName + '\''+
                    '}';
        }
    }

    enum Action {
        DELETE, MOVE, RENAME
    }

    private static class LogEntry {
        public String previousPath = null;
        public String currentPath = null;
        public Action action = null;

        public LogEntry(String currPath, String prevPath, Action action) throws IllegalArgumentException {
            if((action == Action.MOVE || action == Action.RENAME) && prevPath == null){
                throw new IllegalArgumentException("prevPath argument cannot be null for MOVE or RENAME action");
            }
            this.previousPath = prevPath;
            this.currentPath = currPath;
            this.action = action;
            this.writeToDatabase(prevPath, currPath, action);
        }

        public boolean writeToDatabase(String previousPath, String currPath, Action action){
            int op = 0;
            switch(action){
                case DELETE:{
                    op = OpsLog.FILE_DELETED;
                    break;
                }
                case RENAME:{
                    op = OpsLog.FILE_RENAMED;
                    break;
                }
                case MOVE:{
                    op = OpsLog.FILE_MOVED;
                    break;
                }
            }
            OpsLog.Result res = FileMovementLog.dbLog.enqueue(op, null, previousPath, currPath);
            if(res.success){
                FileMovementLog.instance.removeFirstEntry();
            }
            return res.success;
        }

        @Override
        public String toString() {
            return "PreviousPath= " + previousPath +
                    ", \nCurrentPath= " + currentPath +
                    ", \nAction=" + action;
        }
    }

    private static FileMovementLog instance = null;

    private Queue<Movement> movementQ = new LinkedList<>();
    private Queue<LogEntry> entryQ = new LinkedList<>();
    private boolean renameFlag = false;
    private static OpsLog dbLog;

    private FileMovementLog(Context ctx){
        dbLog = new OpsLog(ctx, MemoryDB.getInstance(ctx));
    }

    public static FileMovementLog getInstance(Context ctx){
        if(FileMovementLog.instance == null){
            instance = new FileMovementLog(ctx);
        }
        return instance;
    }

    public void removeFirstEntry(){
        entryQ.remove();
    }

    public void recordMovement(WatchEvent<?> event, String path, WatchEvent<?> nextEvent){
        Movement curr = new Movement(event, path, event.context().toString());

        if(renameFlag) {
            renameFlag = false;
            if(!curr.event.equals("ENTRY_CREATE")) return;
            Movement prev = movementQ.remove();
            entryQ.add(new LogEntry(curr.absolutePath(), prev.absolutePath(), Action.RENAME));
            return;
        }

        if(movementQ.isEmpty()){
            if(curr.event.equals("ENTRY_DELETE")){
                renameFlag = (nextEvent != null);
                movementQ.add(curr);
            }
            return;
        }

        Movement prev = movementQ.peek();

        if(prev.event.equals("ENTRY_DELETE") && curr.event.equals("ENTRY_CREATE")){
            /*
                PREV -> DELETE , CURR -> CREATE
                Either file is renamed or moved
                Or separate actions on both
             */
            boolean sameFileName = prev.fileName.equals(curr.fileName);
            boolean sameParentDir = prev.parentDir.equals(curr.parentDir);
            if(sameFileName && !sameParentDir){
                // file moved from prev.parentDir to curr.parentDir
                entryQ.add(new LogEntry(curr.absolutePath(), prev.absolutePath(), Action.MOVE));
            } else {
                // Previous is deleted and current is created separately
                entryQ.add(new LogEntry(prev.absolutePath(), null, Action.DELETE));
            }
            movementQ.remove();
        } else if (prev.event.equals(curr.event)){
            /*
                PREV = CURR = ENTRY_DELETE
                Delete the prev file and add the current file to movementQ
            */
            if(prev.event.equals("ENTRY_DELETE")){
                // delete the prev and add the current to movementQ
                entryQ.add(new LogEntry(prev.absolutePath(), null, Action.DELETE));
                movementQ.remove();
                movementQ.add(curr);
            }
        }
    }
}