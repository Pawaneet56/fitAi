package com.pawaneet.fitai.ai.client;

import com.google.genai.Client;
import com.google.genai.Pager;
import com.google.genai.types.*;
import org.springframework.stereotype.Component;

@Component
public class GeminiFileSearchClient {

    private final Client client;

    public GeminiFileSearchClient() {
        this.client = new Client();
    }

    public FileSearchStore createStore(String displayName) {

        CreateFileSearchStoreConfig config =
                CreateFileSearchStoreConfig.builder()
                        .displayName(displayName)
                        .build();

        return client.fileSearchStores.create(config);
    }
    public UploadToFileSearchStoreOperation uploadDocument(
            String storeName,
            byte[] content,
            String displayName
    ) {

        UploadToFileSearchStoreConfig config =
                UploadToFileSearchStoreConfig.builder()
                        .displayName(displayName)
                        .mimeType("text/plain")
                        .build();

        return client.fileSearchStores.uploadToFileSearchStore(
                storeName,
                content,
                config
        );
    }
    public FileSearchStore getStore(String storeName) {
        return client.fileSearchStores.get(storeName, null);
    }

    public Pager<Document> listDocuments(String storeName) {
        return client.fileSearchStores.documents.list(
                storeName,
                ListDocumentsConfig.builder().build()
        );
    }
}