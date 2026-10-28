package com.doulgs.testyourluck.media;

import com.doulgs.testyourluck.shared.tenant.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "media_files")
public class MediaFile extends BaseEntity {

    @Column(name = "content_type", nullable = false, length = 60)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;
}
