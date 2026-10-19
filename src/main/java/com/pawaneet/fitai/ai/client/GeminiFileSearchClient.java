package com.pawaneet.fitai.ai.client;

import com.google.genai.Client;
import com.google.genai.Pager;
import com.google.genai.types.*;
import org.springframework.stereotype.Component;

@Component
public class GeminiFileSearchClient {

    private final Client client;

    public GeminiFileSearchClient() {
        this.client = Client.builder().httpOptions(HttpOptions.builder().timeout(30000).build()).build();
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

    public UploadToFileSearchStoreOperation uploadWorkout(String storeName, byte[] content,
            String displayName, java.util.List<CustomMetadata> metadata) {
        // SDK 1.44's direct store uploader drops displayName/customMetadata.
        // Upload a named temporary file, then import with metadata via the JSON API.
        File file = client.files.upload(content, UploadFileConfig.builder()
                .displayName(displayName).mimeType("text/plain").build());
        ImportFileOperation operation = client.fileSearchStores.importFile(storeName,
                file.name().orElseThrow(), ImportFileConfig.builder().customMetadata(metadata).build());
        // Both operation responses expose documentName; keep one persisted operation contract.
        return UploadToFileSearchStoreOperation.fromJson(operation.toJson());
    }

    public UploadToFileSearchStoreOperation getUploadOperation(String name) {
        return client.operations.get(UploadToFileSearchStoreOperation.builder().name(name).build(), null);
    }

    public Document getDocument(String name) {
        return client.fileSearchStores.documents.get(name, null);
    }
    public void deleteDocument(String name) {
        client.fileSearchStores.documents.delete(name, DeleteDocumentConfig.builder().force(true).build());
    }
}