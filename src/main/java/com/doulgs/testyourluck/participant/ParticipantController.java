package com.doulgs.testyourluck.participant;

import com.doulgs.testyourluck.participant.ParticipantDtos.ParticipantRequest;
import com.doulgs.testyourluck.participant.ParticipantDtos.ParticipantResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipantResponse register(@Valid @RequestBody ParticipantRequest req) {
        return service.register(req);
    }
}
