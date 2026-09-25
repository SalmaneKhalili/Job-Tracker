package org.salmanekhalili.jobtrack.service;

import lombok.RequiredArgsConstructor;
import org.salmanekhalili.jobtrack.domain.Application;
import org.salmanekhalili.jobtrack.domain.ApplicationRepository;
import org.salmanekhalili.jobtrack.domain.Note;
import org.salmanekhalili.jobtrack.domain.NoteRepository;
import org.salmanekhalili.jobtrack.domain.UserRepository;
import org.salmanekhalili.jobtrack.dto.NoteRequest;
import org.salmanekhalili.jobtrack.dto.NoteResponse;
import org.salmanekhalili.jobtrack.exception.ApplicationNotFoundException;
import org.salmanekhalili.jobtrack.exception.NoteNotFoundException;
import org.salmanekhalili.jobtrack.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    public List<NoteResponse> listForApplication(Long applicationId) {
        requireOwnedApplication(applicationId);
        return noteRepository
                .findAllByApplicationIdAndApplicationUserIdOrderByCreatedAtDescIdDesc(
                        applicationId, currentUser.requireId())
                .stream()
                .map(NoteService::toResponse)
                .toList();
    }

    @Transactional
    public NoteResponse add(Long applicationId, NoteRequest request) {
        Note note = new Note();
        note.setApplication(requireOwnedApplication(applicationId));
        note.setBody(request.body().trim());
        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public NoteResponse update(Long noteId, NoteRequest request) {
        Note note = requireOwned(noteId);
        note.setBody(request.body().trim());
        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public void delete(Long noteId) {
        noteRepository.delete(requireOwned(noteId));
    }

    private Application requireOwnedApplication(Long applicationId) {
        return applicationRepository.findByIdAndUserId(applicationId, currentUser.requireId())
                .orElseThrow(() -> new ApplicationNotFoundException(applicationId));
    }

    private Note requireOwned(Long noteId) {
        return noteRepository.findByIdAndApplicationUserId(noteId, currentUser.requireId())
                .orElseThrow(() -> new NoteNotFoundException(noteId));
    }

    static NoteResponse toResponse(Note note) {
        return new NoteResponse(note.getId(), note.getApplication().getId(), note.getBody(), note.getCreatedAt());
    }
}
