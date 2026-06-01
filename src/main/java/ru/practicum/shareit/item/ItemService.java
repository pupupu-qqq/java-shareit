package ru.practicum.shareit.item;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ItemService {
    private final UserService userService;
    private final Map<Long, Item> items = new LinkedHashMap<>();
    private long nextId = 1L;

    public ItemService(UserService userService) {
        this.userService = userService;
    }

    public ItemDto create(Long ownerId, ItemDto itemDto) {
        User owner = userService.getUser(ownerId);
        validateNewItem(itemDto);

        Item item = new Item(
                nextId++,
                itemDto.getName(),
                itemDto.getDescription(),
                itemDto.getAvailable(),
                owner,
                null
        );
        items.put(item.getId(), item);

        return ItemMapper.toItemDto(item);
    }

    public ItemDto update(Long ownerId, Long itemId, ItemDto itemDto) {
        userService.getUser(ownerId);
        Item item = getItem(itemId);
        if (!item.getOwner().getId().equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found for owner");
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

        return ItemMapper.toItemDto(item);
    }

    public ItemDto getById(Long itemId) {
        return ItemMapper.toItemDto(getItem(itemId));
    }

    public Collection<ItemDto> getByOwner(Long ownerId) {
        userService.getUser(ownerId);

        return items.values().stream()
                .filter(item -> item.getOwner().getId().equals(ownerId))
                .map(ItemMapper::toItemDto)
                .toList();
    }

    public Collection<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String searchText = text.toLowerCase(Locale.ROOT);
        return items.values().stream()
                .filter(Item::isAvailable)
                .filter(item -> containsIgnoreCase(item.getName(), searchText)
                        || containsIgnoreCase(item.getDescription(), searchText))
                .map(ItemMapper::toItemDto)
                .toList();
    }

    private Item getItem(Long itemId) {
        Item item = items.get(itemId);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found");
        }
        return item;
    }

    private void validateNewItem(ItemDto itemDto) {
        validateText(itemDto.getName(), "Item name");
        validateText(itemDto.getDescription(), "Item description");
        if (itemDto.getAvailable() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item availability is required");
        }
    }

    private void validateText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
    }

    private boolean containsIgnoreCase(String value, String searchText) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(searchText);
    }
}
