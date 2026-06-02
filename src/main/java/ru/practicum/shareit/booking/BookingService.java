package ru.practicum.shareit.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.exception.AccessDeniedException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserService;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

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
        if (approved == null) {
            throw new ValidationException("Approval value is required");
        }

        Booking booking = getBooking(bookingId);
        if (!booking.getItem().getOwner().getId().equals(ownerId)) {
            throw new AccessDeniedException("Only item owner can approve booking");
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new ValidationException("Booking has already been approved or rejected");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return BookingMapper.toBookingDto(bookingRepository.save(booking));
    }

    public BookingDto getById(Long userId, Long bookingId) {
        userService.checkUserExists(userId);
        Booking booking = getBooking(bookingId);
        boolean isBooker = booking.getBooker().getId().equals(userId);
        boolean isOwner = booking.getItem().getOwner().getId().equals(userId);
        if (!isBooker && !isOwner) {
            throw new NotFoundException("Booking not found for user");
        }

        return BookingMapper.toBookingDto(booking);
    }

    public Collection<BookingDto> getByBooker(Long bookerId, BookingState state) {
        userService.checkUserExists(bookerId);
        LocalDateTime now = LocalDateTime.now();
        Map<BookingState, Supplier<List<Booking>>> handlers = Map.of(
                BookingState.ALL, () -> bookingRepository.findByBooker_Id(bookerId, START_DESC),
                BookingState.CURRENT, () -> bookingRepository.findByBooker_IdAndStartBeforeAndEndAfter(
                        bookerId,
                        now,
                        now,
                        START_DESC
                ),
                BookingState.PAST, () -> bookingRepository.findByBooker_IdAndEndBefore(bookerId, now, START_DESC),
                BookingState.FUTURE, () -> bookingRepository.findByBooker_IdAndStartAfter(bookerId, now, START_DESC),
                BookingState.WAITING, () -> bookingRepository.findByBooker_IdAndStatus(
                        bookerId,
                        BookingStatus.WAITING,
                        START_DESC
                ),
                BookingState.REJECTED, () -> bookingRepository.findByBooker_IdAndStatus(
                        bookerId,
                        BookingStatus.REJECTED,
                        START_DESC
                )
        );

        return toBookingDtos(fetchByState(state, handlers));
    }

    public Collection<BookingDto> getByOwner(Long ownerId, BookingState state) {
        userService.checkUserExists(ownerId);
        LocalDateTime now = LocalDateTime.now();
        Map<BookingState, Supplier<List<Booking>>> handlers = Map.of(
                BookingState.ALL, () -> bookingRepository.findByItem_Owner_Id(ownerId, START_DESC),
                BookingState.CURRENT, () -> bookingRepository.findByItem_Owner_IdAndStartBeforeAndEndAfter(
                        ownerId,
                        now,
                        now,
                        START_DESC
                ),
                BookingState.PAST, () -> bookingRepository.findByItem_Owner_IdAndEndBefore(ownerId, now, START_DESC),
                BookingState.FUTURE, () -> bookingRepository.findByItem_Owner_IdAndStartAfter(ownerId, now, START_DESC),
                BookingState.WAITING, () -> bookingRepository.findByItem_Owner_IdAndStatus(
                        ownerId,
                        BookingStatus.WAITING,
                        START_DESC
                ),
                BookingState.REJECTED, () -> bookingRepository.findByItem_Owner_IdAndStatus(
                        ownerId,
                        BookingStatus.REJECTED,
                        START_DESC
                )
        );

        return toBookingDtos(fetchByState(state, handlers));
    }

    private Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
    }

    private List<Booking> fetchByState(BookingState state, Map<BookingState, Supplier<List<Booking>>> handlers) {
        BookingState bookingState = state == null ? BookingState.ALL : state;
        return handlers.get(bookingState).get();
    }

    private Collection<BookingDto> toBookingDtos(List<Booking> bookings) {
        return bookings.stream()
                .map(BookingMapper::toBookingDto)
                .toList();
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
