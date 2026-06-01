package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {
    private static final Sort START_DESC = Sort.by(Sort.Direction.DESC, "start");

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final ItemService itemService;

    @Transactional
    public BookingDto create(Long bookerId, BookingCreateDto bookingCreateDto) {
        validateBookingCreateDto(bookingCreateDto);
        User booker = userService.getUser(bookerId);
        Item item = itemService.getItem(bookingCreateDto.getItemId());
        validateBookingCreate(booker, item);

        Booking booking = new Booking(
                null,
                bookingCreateDto.getStart(),
                bookingCreateDto.getEnd(),
                item,
                booker,
                BookingStatus.WAITING
        );

        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    @Transactional
    public BookingDto approve(Long ownerId, Long bookingId, Boolean approved) {
        userService.getUser(ownerId);
        if (approved == null) {
            throw new ValidationException("Approval value is required");
        }

        Booking booking = getBooking(bookingId);
        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new NotFoundException("Booking not found for owner");
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Booking has already been approved or rejected");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    public BookingDto getById(Long userId, Long bookingId) {
        userService.getUser(userId);
        Booking booking = getBooking(bookingId);
        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);
        if (!isBooker && !isOwner) {
            throw new NotFoundException("Booking not found for user");
        }

        return BookingMapper.toBookingDto(booking);
    }

    public Collection<BookingDto> getByBooker(Long bookerId, String state) {
        userService.getUser(bookerId);
        BookingState bookingState = BookingState.from(state);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (bookingState) {
            case ALL -> bookingRepository.findByBooker_Id(bookerId, START_DESC);
            case CURRENT -> bookingRepository.findByBooker_IdAndStartBeforeAndEndAfter(bookerId, now, now, START_DESC);
            case PAST -> bookingRepository.findByBooker_IdAndEndBefore(bookerId, now, START_DESC);
            case FUTURE -> bookingRepository.findByBooker_IdAndStartAfter(bookerId, now, START_DESC);
            case WAITING -> bookingRepository.findByBooker_IdAndStatus(bookerId, BookingStatus.WAITING, START_DESC);
            case REJECTED -> bookingRepository.findByBooker_IdAndStatus(bookerId, BookingStatus.REJECTED, START_DESC);
        };

        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    public Collection<BookingDto> getByOwner(Long ownerId, String state) {
        userService.getUser(ownerId);
        BookingState bookingState = BookingState.from(state);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (bookingState) {
            case ALL -> bookingRepository.findByItem_Owner_Id(ownerId, START_DESC);
            case CURRENT -> bookingRepository.findByItem_Owner_IdAndStartBeforeAndEndAfter(ownerId, now, now, START_DESC);
            case PAST -> bookingRepository.findByItem_Owner_IdAndEndBefore(ownerId, now, START_DESC);
            case FUTURE -> bookingRepository.findByItem_Owner_IdAndStartAfter(ownerId, now, START_DESC);
            case WAITING -> bookingRepository.findByItem_Owner_IdAndStatus(ownerId, BookingStatus.WAITING, START_DESC);
            case REJECTED -> bookingRepository.findByItem_Owner_IdAndStatus(ownerId, BookingStatus.REJECTED, START_DESC);
        };

        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
    }

    private Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private void validateBookingCreateDto(BookingCreateDto bookingCreateDto) {
        if (bookingCreateDto == null || bookingCreateDto.getItemId() == null) {
            throw new ValidationException("Booking item is required");
        }
        if (bookingCreateDto.getStart() == null || bookingCreateDto.getEnd() == null) {
            throw new ValidationException("Booking dates are required");
        }
        if (!bookingCreateDto.getEnd().isAfter(bookingCreateDto.getStart())) {
            throw new ValidationException("Booking end must be after start");
        }
    }

    private void validateBookingCreate(User booker, Item item) {
        if (!item.isAvailable()) {
            throw new ValidationException("Item is not available");
        }
        if (item.getOwner().getId().equals(booker.getId())) {
            throw new NotFoundException("Owner cannot book own item");
        }
    }
}
