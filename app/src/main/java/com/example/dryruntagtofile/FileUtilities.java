package com.example.dryruntagtofile;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContentResolverCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;

public class FileUtilities {
    static int SUCCESS = 0xf0000000;
    static int FILE_ALREADY_EXISTS = 0x000000ff;
    static int NON_EMPTY_DIRECTORY = 0x00000f0f;
    static int DIRECTORY_REQUIRED = 0X0000f00f;
    static int FILE_REQUIRED = 0x000f000f;
    static int FILE_MISSING = 0x00f0000f;
    static int FOLDER_MISSING = 0x0f00000f;
    static int FAILED = 0x0000000f;

    static File getFileRoot(Context ctx){
        Cursor c = ctx.getContentResolver().query(MediaStore.Files.getContentUri("external_primary"), new String[]{MediaStore.Files.FileColumns.DATA}, MediaStore.Files.FileColumns.DISPLAY_NAME+"=?", new String[]{"Android"}, null);
        c.moveToFirst();
        return new File(c.getString(0)).getParentFile();
    }

    static int deleteFile(Path src) {
        try {
            Files.delete(src);
            return SUCCESS;
        }
        catch (FileNotFoundException e){
            return FILE_MISSING;
        }
        catch (DirectoryNotEmptyException e){
            return NON_EMPTY_DIRECTORY;
        }
        catch(Exception e){
            return FAILED;
        }
    }

    static int copyFileOrFolder(Path src, Path dest) {
        try {
            if(dest.toFile().isDirectory()){
                dest = Paths.get(dest.toString(), src.getFileName().toString());
            }
            Files.copy(src, dest);
            return SUCCESS;
        }
        catch (FileAlreadyExistsException e){
            return FILE_ALREADY_EXISTS;
        }
        catch (DirectoryNotEmptyException e){
            return NON_EMPTY_DIRECTORY;
        }
        catch(Exception e){
            return FAILED;
        }
    }

    static int replaceFile(Path src, Path destDir) {
        try {
            if(src.toFile().isDirectory()){
                return FAILED | FILE_REQUIRED;
            }
            if(!destDir.toFile().isDirectory()){
                return FAILED | DIRECTORY_REQUIRED;
            }
            Files.copy(src, destDir, StandardCopyOption.REPLACE_EXISTING);
            return SUCCESS;
        }
        catch(DirectoryNotEmptyException e){
            e.printStackTrace();
            return NON_EMPTY_DIRECTORY;
        }
        catch (Exception e){
            e.printStackTrace();
            return FAILED;
        }
    }

    // folder implementation pending
    static int moveFile(Path src, Path dest, boolean atomicMoveFlag, boolean replaceFlag){
        try {
            System.out.println(dest.toAbsolutePath());
            if(!dest.toFile().isDirectory()) {
                return FAILED | DIRECTORY_REQUIRED;
            }
            dest = Paths.get(dest.toAbsolutePath().toString(), src.getFileName().toString());
            if(atomicMoveFlag && replaceFlag){
                Files.move(src, dest, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } else if (atomicMoveFlag){
                Files.move(src, dest, StandardCopyOption.ATOMIC_MOVE);
            } else if(replaceFlag){
                Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(src, dest);
            }
            return SUCCESS;
        } catch(FileAlreadyExistsException e){
            e.printStackTrace();
            return FILE_ALREADY_EXISTS;
        } catch(DirectoryNotEmptyException e){
            e.printStackTrace();
            return NON_EMPTY_DIRECTORY;
        } catch(IOException e){
            e.printStackTrace();
            return FAILED;
        }
    }

    static int renameFileOrFolder(Path src, String newName) {
        try {
            if (!src.toFile().exists()) {
                return FAILED | FILE_MISSING;
            }
            if (newName.length() < 1) {
                return FAILED | FILE_MISSING;
            }
            String fileName = src.getFileName().toString();
            Path parentDir = src.getParent().toAbsolutePath();
            if (src.toFile().isFile()) {
                String fileType = fileName.substring(fileName.lastIndexOf('.'));
                newName = newName + fileType;
            }
            Path dest = Paths.get(parentDir.toString(), newName);
            System.out.println(src.toAbsolutePath());
            System.out.println(dest.toAbsolutePath());
            Files.move(src, dest);
            return SUCCESS;
        }
        catch(FileAlreadyExistsException e){
            e.printStackTrace();
            return FILE_ALREADY_EXISTS;
        } catch(DirectoryNotEmptyException e){
            e.printStackTrace();
            return NON_EMPTY_DIRECTORY;
        } catch(IOException e){
            e.printStackTrace();
            return FAILED;
        }
    }
}
