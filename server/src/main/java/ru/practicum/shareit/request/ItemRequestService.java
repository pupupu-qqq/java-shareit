package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.RequestItemDto;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemRequestService {
    private final ItemRequestRepository itemRequestRepository;
    private final ItemRepository itemRepository;
    private final UserService userService;

    @Transactional
    public ItemRequestDto create(Long userId, ItemRequestDto requestDto) {
        User requestor = userService.getUser(userId);
        validateDescription(requestDto);

        ItemRequest request = new ItemRequest(
                null,
                requestDto.getDescription(),
                requestor,
                LocalDateTime.now()
        );

        return ItemRequestMapper.toItemRequestDto(itemRequestRepository.save(request), List.of());
    }

    public Collection<ItemRequestDto> getByRequestor(Long userId) {
        userService.checkUserExists(userId);
        List<ItemRequest> requests = itemRequestRepository.findByRequestor_IdOrderByCreatedDesc(userId);
        return toItemRequestDtos(requests);
    }

    public Collection<ItemRequestDto> getAll(Long userId, Integer from, Integer size) {
        userService.checkUserExists(userId);
        validatePage(from, size);

        List<ItemRequest> requests = itemRequestRepository.findByRequestor_IdNotOrderByCreatedDesc(userId);
        return toItemRequestDtos(page(requests, from, size));
    }

    public ItemRequestDto getById(Long userId, Long requestId) {
        userService.checkUserExists(userId);
        ItemRequest request = itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Item request not found"));

        return ItemRequestMapper.toItemRequestDto(
                request,
                itemRepository.findByRequest_Id(requestId).stream()
                        .map(ItemRequestMapper::toRequestItemDto)
                        .toList()
        );
    }

    private Collection<ItemRequestDto> toItemRequestDtos(List<ItemRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        Map<Long, List<RequestItemDto>> itemsByRequestId = itemRepository.findByRequest_IdIn(
                        requests.stream()
                                .map(ItemRequest::getId)
                                .toList()
                ).stream()
                .collect(Collectors.groupingBy(
                        item -> item.getRequest().getId(),
                        Collectors.mapping(ItemRequestMapper::toRequestItemDto, Collectors.toList())
                ));

        return requests.stream()
                .map(request -> ItemRequestMapper.toItemRequestDto(
                        request,
                        itemsByRequestId.getOrDefault(request.getId(), List.of())
                ))
                .toList();
    }

    private List<ItemRequest> page(List<ItemRequest> requests, Integer from, Integer size) {
        if (from == null || size == null) {
            return requests;
        }
        if (from >= requests.size()) {
            return List.of();
        }

        int toIndex = Math.min(from + size, requests.size());
        return requests.subList(from, toIndex);
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
