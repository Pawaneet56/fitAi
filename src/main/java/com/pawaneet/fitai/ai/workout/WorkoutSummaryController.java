package com.pawaneet.fitai.ai.workout;

import com.google.genai.Pager;
import com.google.genai.types.Document;
import com.google.genai.types.FileSearchStore;
import com.google.genai.types.UploadToFileSearchStoreOperation;
import com.pawaneet.fitai.ai.dto.WorkoutSummaryResponse;
import com.pawaneet.fitai.ai.service.FileSearchStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/workouts")
@RequiredArgsConstructor
public class WorkoutSummaryController {

    private final WorkoutSummaryService workoutSummaryService;

    private final FileSearchStoreService fileSearchStoreService;
    @GetMapping("/{workoutId}/summary")
    public ResponseEntity<WorkoutSummaryResponse> generateSummary(
            @PathVariable UUID workoutId) {

        return ResponseEntity.ok(
                workoutSummaryService.generateSummary(workoutId)
        );
    }
    @PostMapping("/file-test")
    public FileSearchStore createStore(
            @RequestParam String displayName) {

        return fileSearchStoreService.createStore(displayName);
    }

    @PostMapping("/documents")
    public UploadToFileSearchStoreOperation uploadDocument(
            @RequestParam String displayName,
            @RequestBody String content) {

        return fileSearchStoreService.uploadDocument(
                content.getBytes(StandardCharsets.UTF_8),
                displayName
        );
    }

    @GetMapping("/documents")
    public Pager<Document> listDocuments() {
        return fileSearchStoreService.listDocuments();
    }

}