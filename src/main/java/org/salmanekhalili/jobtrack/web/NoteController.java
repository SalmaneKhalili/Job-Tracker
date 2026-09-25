package org.salmanekhalili.jobtrack.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.salmanekhalili.jobtrack.dto.NoteRequest;
import org.salmanekhalili.jobtrack.dto.NoteResponse;
import org.salmanekhalili.jobtrack.service.NoteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @GetMapping("/applications/{applicationId}/notes")
    public List<NoteResponse> list(@PathVariable Long applicationId) {
        return noteService.listForApplication(applicationId);
    }

    @PostMapping("/applications/{applicationId}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse add(@PathVariable Long applicationId, @Valid @RequestBody NoteRequest request) {
        return noteService.add(applicationId, request);
    }

    @PutMapping("/notes/{noteId}")
    public NoteResponse update(@PathVariable Long noteId, @Valid @RequestBody NoteRequest request) {
        return noteService.update(noteId, request);
    }

    @DeleteMapping("/notes/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long noteId) {
        noteService.delete(noteId);
    }
}
