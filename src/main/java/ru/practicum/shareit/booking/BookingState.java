package ru.practicum.shareit.booking;

import ru.practicum.shareit.exception.ValidationException;

public enum BookingState {
    ALL,
    CURRENT,
    PAST,
    FUTURE,
    WAITING,
    REJECTED;

    public static BookingState from(String value) {
        if (value == null) {
            throw new ValidationException("Unknown state: null");
        }

        try {
            return BookingState.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("Unknown state: " + value);
        }
    }
}
