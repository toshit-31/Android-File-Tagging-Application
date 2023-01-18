package com.example.dryruntagtofile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.UnaryOperator;

class FileItem {
    public String absolutePath = null;
    public String parentDir = null;
    public String fileName = null;

    private Path filePath = null;

    public FileItem(String path){
        filePath = Paths.get(path);
        absolutePath = filePath.toAbsolutePath().toString();
        parentDir = filePath.getParent().toString();
        fileName = filePath.getFileName().toString();
    }

    public long getModifiedDate(){
        return filePath.toFile().lastModified();
    }

    public long size(){
        return filePath.toFile().length();
    }

    @Override
    public String toString() {
        return filePath.toAbsolutePath().toString();
    }
}

public class FileList {

    private ArrayList<FileItem> fileArray = new ArrayList<>();

    public FileList(String[] fileList){
        for(int i = 0; i < fileList.length; i++){
            fileArray.add(new FileItem(fileList[i]));
        }
    }

    public FileList(FileItem[] fileList){
        for(int i = 0; i < fileList.length; i++){
            fileArray.add(fileList[i]);
        }
    }

    public int count(){
        return fileArray.size();
    }

    public FileItem[] getFileArray() {
        return fileArray.toArray(new FileItem[0]);
    }

    public FileItem[] getFileArray(int limit, int offset) {
        FileItem[] files = new FileItem[limit];
        for(int i = offset; i < offset+limit || i < fileArray.size(); i++){
            files[i] = fileArray.get(i);
        }
        return files;
    }

    public FileList sortList(String by){
        fileArray.sort(new Comparator<FileItem>() {
            @Override
            public int compare(FileItem f1, FileItem f2) {
                switch(by){
                    case "date": {
                        return (int)(f1.getModifiedDate() - f2.getModifiedDate());
                    }
                    case "size": {
                        return (int)(f1.size() - f2.size());
                    }
                    default : {
                        return f1.fileName.compareToIgnoreCase(f2.fileName);
                    }
                }
            }
        });
        return this;
    }
}
