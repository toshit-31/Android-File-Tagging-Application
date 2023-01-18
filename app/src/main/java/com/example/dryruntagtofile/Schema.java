package com.example.dryruntagtofile;

public class Schema {
    public static String tagToFiles = "tag_to_files(" +
            "tag_uid INTEGER PRIMARY KEY," +
            "file_list TEXT)";

    public static String tagList = "tags(" +
            "tag_uid INTEGER PRIMARY KEY," +
            "tag_name TEXT UNIQUE); " +
            "CREATE INDEX tag_shortener ON tags(tag_name)";

    public static String fileToTags = "file_to_tags(" +
            "absolute_path TEXT PRIMARY KEY," +
            "tags TEXT)";

    public static String opsLog = "ops_log(" +
            "id INTEGER PRIMARY KEY," +
            "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "operation INTEGER," +
            "data_json TEXT," +
            "completed BOOLEAN)";

    public static String tableTags = "tags";
    public static String tableTagToFiles = "tag_to_files";
    public static String tableFileToTags = "file_to_tags";
    public static String tableOps = "ops_log";

    public static class Tags {
        public static final String tag_uid = "tag_uid";
        public static final String tagName = "tag_name";
    }

    public static class TagToFiles {
        public static final String tag_uid = "tag_uid";
        public static final String fileList = "file_list";
    }

    public static class FileToTags {
        public static final String filePath = "absolute_path";
        public static final String tags = "tags";
    }

    public static class OpsLog {
        public static final String op = "operation";
        public static final String data = "data_json";
        public static final String completed = "completed";
    }
}
