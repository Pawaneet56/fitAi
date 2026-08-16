package com.pawaneet.fitai.ai.service;

import com.google.genai.Pager;
import com.google.genai.types.Document;
import com.google.genai.types.FileSearchStore;
import com.google.genai.types.UploadToFileSearchStoreOperation;
import com.pawaneet.fitai.ai.client.GeminiFileSearchClient;
import com.pawaneet.fitai.ai.config.FileSearchProperties;
import org.springframework.stereotype.Service;

@Service
public class FileSearchStoreService {

    private final GeminiFileSearchClient fileSearchClient;
    private final FileSearchProperties fileSearchProperties;
    public FileSearchStoreService(GeminiFileSearchClient fileSearchClient, FileSearchProperties fileSearchProperties) {
        this.fileSearchClient = fileSearchClient;
        this.fileSearchProperties=fileSearchProperties;
    }

    public FileSearchStore createStore(String displayName) {
        return fileSearchClient.createStore(displayName);
    }

    public UploadToFileSearchStoreOperation uploadDocument(
            byte[] content,
            String displayName) {

        return fileSearchClient.uploadDocument(
                fileSearchProperties.storeName(),
                content,
                displayName
        );
    }
    public Pager<Document> listDocuments() {
        return fileSearchClient.listDocuments(fileSearchProperties.storeName());
    }
}