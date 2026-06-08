package com.kv.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;

public class KVStore {
    private static final Logger log = LoggerFactory.getLogger(KVStore.class);
    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
    private final String persistPath;

    public KVStore(String persistPath) {
        this.persistPath = persistPath;
        loadFromDisk();
    }

    public void put(String key, String value) {
        store.put(key, value);
        persistAsync(key, value);
    }

    public String get(String key) {
        return store.get(key);
    }

    public void delete(String key) {
        store.remove(key);
        persistDelete(key);
    }

    private void persistAsync(String key, String value) {
        // En producción usar un executor, aquí simplificamos escritura sincrónica
        try (FileWriter fw = new FileWriter(persistPath + "/" + key + ".txt", true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(value);
            bw.newLine();
        } catch (IOException e) {
            log.error("Failed to persist", e);
        }
    }

    private void persistDelete(String key) {
        new File(persistPath + "/" + key + ".txt").delete();
    }

    private void loadFromDisk() {
        File dir = new File(persistPath);
        if (!dir.exists()) dir.mkdirs();
        for (File file : dir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".txt")) {
                String key = file.getName().replace(".txt", "");
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String value = br.readLine();
                    if (value != null) store.put(key, value);
                } catch (IOException e) {
                    log.error("Failed to load " + file, e);
                }
            }
        }
    }
}