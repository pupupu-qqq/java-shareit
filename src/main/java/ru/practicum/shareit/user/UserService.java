package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public UserDto create(UserDto userDto) {
        validateEmail(userDto.getEmail());
        checkEmailIsFree(userDto.getEmail(), null);

        User user = new User(null, userDto.getName(), userDto.getEmail());

        return UserMapper.toUserDto(userRepository.save(user));
    }

    @Transactional
    public UserDto update(Long userId, UserDto userDto) {
        User user = getUser(userId);

        if (userDto.getEmail() != null) {
            validateEmail(userDto.getEmail());
            checkEmailIsFree(userDto.getEmail(), userId);
            user.setEmail(userDto.getEmail());
        }
        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }

        return UserMapper.toUserDto(user);
    }

    public UserDto getById(Long userId) {
        return UserMapper.toUserDto(getUser(userId));
    }

    public Collection<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Transactional
    public void delete(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        }
    }

    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ValidationException("Invalid email");
        }
    }

    private void checkEmailIsFree(String email, Long currentUserId) {
        boolean emailExists = currentUserId == null
                ? userRepository.existsByEmail(email)
                : userRepository.existsByEmailAndIdNot(email, currentUserId);

        if (emailExists) {
            throw new ConflictException("Email already exists");
        }
    }
}
