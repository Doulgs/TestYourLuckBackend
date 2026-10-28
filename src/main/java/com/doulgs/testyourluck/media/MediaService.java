package com.doulgs.testyourluck.media;

import com.doulgs.testyourluck.shared.tenant.TenantContext;
import com.doulgs.testyourluck.shared.web.NotFoundException;
import com.doulgs.testyourluck.shared.web.PayloadTooLargeException;
import com.doulgs.testyourluck.shared.web.UnsupportedMediaException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final String UNSUPPORTED = "Formato de imagem não suportado";

    public record MediaResponse(UUID id, String url, String contentType, long size) {
    }

    public record StoredMedia(MediaFile file, byte[] content) {
    }

    private final MediaRepository repository;
    private final MediaStorage storage;

    @Value("${app.public-base-url:http://localhost:9090}")
    private String publicBaseUrl;

    @Transactional
    public MediaResponse upload(MultipartFile file) {
        if (file.getSize() > MAX_BYTES) {
            throw new PayloadTooLargeException("Imagem maior que 5 MB");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        String type = detect(bytes);
        MediaFile media = new MediaFile();
        media.setContentType(type);
        media.setSizeBytes(bytes.length);
        media.setOriginalName(file.getOriginalFilename());
        media.setStorageKey(TenantContext.require().tenant() + "/" + media.getId());
        storage.write(media.getStorageKey(), bytes);
        repository.save(media);
        return new MediaResponse(media.getId(), url(media.getId()), type, bytes.length);
    }

    /** Leitura pública: o id aleatório funciona como capacidade de acesso. */
    @Transactional(readOnly = true)
    public StoredMedia read(UUID id) {
        MediaFile media = repository.findById(id).orElseThrow(() -> new NotFoundException("Imagem não encontrada"));
        return new StoredMedia(media, storage.read(media.getStorageKey()));
    }

    /** Garante que a mídia referenciada pertence ao tenant corrente (o filtro @TenantId esconde as demais). */
    public UUID requireOwn(UUID id) {
        if (id != null && !repository.existsById(id)) {
            throw new NotFoundException("Imagem não encontrada");
        }
        return id;
    }

    public String url(UUID id) {
        return id == null ? null : publicBaseUrl + "/api/media/" + id;
    }

    /** Valida o tipo pelos bytes iniciais, sem confiar no content-type enviado. */
    static String detect(byte[] b) {
        if (b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return "image/png";
        }
        if (b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        String head = new String(b, 0, Math.min(b.length, 1024), StandardCharsets.UTF_8).toLowerCase();
        if (head.contains("<svg")) {
            return "image/svg+xml";
        }
        throw new UnsupportedMediaException(UNSUPPORTED);
    }
}
