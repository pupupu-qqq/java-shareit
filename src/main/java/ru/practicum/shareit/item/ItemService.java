package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.BookingRepository;
import ru.practicum.shareit.booking.BookingStatus;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemService {
    private final UserService userService;
    private final ItemRepository itemRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public ItemDto create(Long ownerId, ItemDto itemDto) {
        User owner = userService.getUser(ownerId);
        validateNewItem(itemDto);

        Item item = new Item(
                null,
                itemDto.getName(),
                itemDto.getDescription(),
                itemDto.getAvailable(),
                owner,
                null
        );

        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    @Transactional
    public ItemDto update(Long ownerId, Long itemId, ItemDto itemDto) {
        userService.getUser(ownerId);
        Item item = getItem(itemId);
        if (!item.getOwner().getId().equals(ownerId)) {
            throw new NotFoundException("Item not found for owner");
        }

        if (itemDto.getName() != null) {
            validateText(itemDto.getName(), "Item name");
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            validateText(itemDto.getDescription(), "Item description");
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        return ItemMapper.toItemDto(itemRepository.save(item));
    }

    public ItemDto getById(Long userId, Long itemId) {
        if (userId != null) {
            userService.getUser(userId);
        }

        return toItemDto(getItem(itemId), userId);
    }

    public Collection<ItemDto> getByOwner(Long ownerId) {
        userService.getUser(ownerId);

        return itemRepository.findByOwner_IdOrderByIdAsc(ownerId).stream()
                .map(item -> toItemDto(item, ownerId))
                .toList();
    }

    public Collection<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        return itemRepository.search(text).stream()
                .map(ItemMapper::toItemDto)
                .toList();
    }

    @Transactional
    public CommentDto addComment(Long authorId, Long itemId, CommentDto commentDto) {
        User author = userService.getUser(authorId);
        Item item = getItem(itemId);
        validateText(commentDto.getText(), "Comment text");

        boolean hasCompletedBooking = bookingRepository.existsByItem_IdAndBooker_IdAndEndBeforeAndStatus(
                itemId,
                authorId,
                LocalDateTime.now(),
                BookingStatus.APPROVED
        );
        if (!hasCompletedBooking) {
            throw new ValidationException("User has no completed booking for this item");
        }

        Comment comment = new Comment(
                null,
                commentDto.getText(),
                item,
                author,
                LocalDateTime.now()
        );

        return ItemMapper.toCommentDto(commentRepository.save(comment));
    }

    public Item getItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found"));
    }

    private void validateNewItem(ItemDto itemDto) {
        validateText(itemDto.getName(), "Item name");
        validateText(itemDto.getDescription(), "Item description");
        if (itemDto.getAvailable() == null) {
            throw new ValidationException("Item availability is required");
        }
    }

    private void validateText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
    }

    private ItemDto toItemDto(Item item, Long viewerId) {
        Booking lastBooking = null;
        Booking nextBooking = null;

        if (viewerId != null && item.getOwner().getId().equals(viewerId)) {
            LocalDateTime now = LocalDateTime.now();
            lastBooking = bookingRepository.findFirstByItem_IdAndEndBeforeAndStatusOrderByEndDesc(
                    item.getId(),
                    now,
                    BookingStatus.APPROVED
            ).orElse(null);
            nextBooking = bookingRepository.findFirstByItem_IdAndStartAfterAndStatusOrderByStartAsc(
                    item.getId(),
                    now,
                    BookingStatus.APPROVED
            ).orElse(null);
        }

        List<CommentDto> comments = commentRepository.findByItem_IdOrderByCreatedAsc(item.getId()).stream()
                .map(ItemMapper::toCommentDto)
                .toList();

        return ItemMapper.toItemDto(item, lastBooking, nextBooking, comments);
    }
}
