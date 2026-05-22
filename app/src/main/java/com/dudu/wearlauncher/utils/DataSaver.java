package com.dudu.wearlauncher.utils;

import com.dudu.wearlauncher.model.WatchFace;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class DataSaver {
    private final File dataJsonFile;
    private JSONObject dataJson = new JSONObject();

    public DataSaver(String watchfaceName) {
        File folder = new File(WatchFace.watchFaceFolder, watchfaceName);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        this.dataJsonFile = new File(folder, "data.json");
        if (!dataJsonFile.exists()) {
            writeString("{}");
        }
        try {
            this.dataJson = new JSONObject(readString());
        } catch (Exception error) {
            ILog.e("data.json is invalid, resetting");
            this.dataJson = new JSONObject();
        }
    }

    public void put(String key, boolean value) { dataJsonPut(key, value); }
    public void put(String key, String value) { dataJsonPut(key, value); }
    public void put(String key, int value) { dataJsonPut(key, value); }
    public void put(String key, double value) { dataJsonPut(key, value); }
    public void put(String key, long value) { dataJsonPut(key, value); }

    public boolean get(String key, boolean defValue) {
        try { return dataJson.getBoolean(key); } catch (Exception ignored) { return defValue; }
    }

    public String get(String key, String defValue) {
        try { return dataJson.getString(key); } catch (Exception ignored) { return defValue; }
    }

    public int get(String key, int defValue) {
        try { return dataJson.getInt(key); } catch (Exception ignored) { return defValue; }
    }

    public long get(String key, long defValue) {
        try { return dataJson.getLong(key); } catch (Exception ignored) { return defValue; }
    }

    public double get(String key, double defValue) {
        try { return dataJson.getDouble(key); } catch (Exception ignored) { return defValue; }
    }

    public void apply() {
        writeString(dataJson.toString());
    }

    private void dataJsonPut(String key, Object value) {
        try {
            dataJson.put(key, value);
        } catch (Exception error) {
            ILog.e("Failed to write key: " + key);
        }
    }

    private String readString() throws Exception {
        byte[] bytes = java.nio.file.Files.readAllBytes(dataJsonFile.toPath());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private void writeString(String content) {
        try {
            FileOutputStream outputStream = new FileOutputStream(dataJsonFile);
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            outputStream.close();
        } catch (Exception error) {
            ILog.e("Failed to persist data.json");
        }
    }
}
