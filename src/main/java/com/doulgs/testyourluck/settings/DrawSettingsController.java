package com.doulgs.testyourluck.settings;

import com.doulgs.testyourluck.settings.DrawSettingsDtos.DrawSettingsRequest;
import com.doulgs.testyourluck.settings.DrawSettingsDtos.DrawSettingsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/draw-settings")
@RequiredArgsConstructor
public class DrawSettingsController {

    private final DrawSettingsService service;

    @GetMapping
    public DrawSettingsResponse get() {
        return service.get();
    }

    @PutMapping
    public DrawSettingsResponse save(@Valid @RequestBody DrawSettingsRequest req) {
        return service.save(req);
    }
}
