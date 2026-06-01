package ru.practicum.shareit.item.mapper;

import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.item.dto.BookingShortDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;

import java.util.List;

public final class ItemMapper {
    private ItemMapper() {
    }

    public static ItemDto toItemDto(Item item) {
        if (item == null) {
            return null;
        }

        return new ItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.isAvailable(),
                item.getRequest() != null ? item.getRequest().getId() : null
        );
    }

    public static ItemDto toItemDto(Item item,
                                    Booking lastBooking,
                                    Booking nextBooking,
                                    List<CommentDto> comments) {
        if (item == null) {
            return null;
        }

        return new ItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.isAvailable(),
                item.getRequest() != null ? item.getRequest().getId() : null,
                toBookingShortDto(lastBooking),
                toBookingShortDto(nextBooking),
                comments
        );
    }

    public static CommentDto toCommentDto(Comment comment) {
        if (comment == null) {
            return null;
        }

        return new CommentDto(
                comment.getId(),
                comment.getText(),
                comment.getAuthor().getName(),
                comment.getCreated()
        );
    }

    private static BookingShortDto toBookingShortDto(Booking booking) {
        if (booking == null) {
            return null;
        }

        return new BookingShortDto(
                booking.getId(),
                booking.getBooker().getId()
        );
    }
}
