package com.doulgs.testyourluck.media;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MediaRepository extends JpaRepository<MediaFile, UUID> {
}
