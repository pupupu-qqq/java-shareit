package ru.practicum.shareit.request;

import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.RequestItemDto;

import java.util.List;

public final class ItemRequestMapper {
    private ItemRequestMapper() {
    }

    public static ItemRequestDto toItemRequestDto(ItemRequest request, List<RequestItemDto> items) {
        if (request == null) {
            return null;
        }

        return new ItemRequestDto(
                request.getId(),
                request.getDescription(),
                request.getCreated(),
                items
        );
    }

    public static RequestItemDto toRequestItemDto(Item item) {
        if (item == null) {
            return null;
        }

        return new RequestItemDto(
                item.getId(),
                item.getName(),
                item.getOwner().getId()
        );
    }
}
