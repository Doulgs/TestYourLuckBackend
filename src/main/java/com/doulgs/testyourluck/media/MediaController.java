package com.doulgs.testyourluck.media;

import com.doulgs.testyourluck.media.MediaService.MediaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MediaResponse upload(@RequestParam("file") MultipartFile file) {
        return mediaService.upload(file);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> read(@PathVariable UUID id) {
        var stored = mediaService.read(id);
        var builder = ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.file().getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff");
        if (stored.file().getContentType().equals("image/svg+xml")) {
            builder.header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'");
        }
        return builder.body(stored.content());
    }
}
