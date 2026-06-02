package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.request.dto.ItemRequestDto;

@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
public class RequestController {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    private final RequestClient requestClient;

    @PostMapping
    public ResponseEntity<Object> create(@RequestHeader(USER_ID_HEADER) Long userId,
                                         @RequestBody ItemRequestDto requestDto) {
        validateDescription(requestDto);
        return requestClient.create(userId, requestDto);
    }

    @GetMapping
    public ResponseEntity<Object> getByRequestor(@RequestHeader(USER_ID_HEADER) Long userId) {
        return requestClient.getByRequestor(userId);
    }

    @GetMapping("/all")
    public ResponseEntity<Object> getAll(@RequestHeader(USER_ID_HEADER) Long userId,
                                         @RequestParam(required = false) Integer from,
                                         @RequestParam(required = false) Integer size) {
        validatePage(from, size);
        return requestClient.getAll(userId, from, size);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<Object> getById(@RequestHeader(USER_ID_HEADER) Long userId,
                                          @PathVariable Long requestId) {
        return requestClient.getById(userId, requestId);
    }

    private void validateDescription(ItemRequestDto requestDto) {
        if (requestDto == null || requestDto.getDescription() == null || requestDto.getDescription().isBlank()) {
            throw new ValidationException("Request description is required");
        }
    }

    private void validatePage(Integer from, Integer size) {
        if (from != null && from < 0) {
            throw new ValidationException("Pagination start must not be negative");
        }
        if (size != null && size <= 0) {
            throw new ValidationException("Pagination size must be positive");
        }
    }
}
