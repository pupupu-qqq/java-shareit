package ru.practicum.shareit.user;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.mapper.UserMapper;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class UserService {
    private final Map<Long, User> users = new LinkedHashMap<>();
    private long nextId = 1L;

    public UserDto create(UserDto userDto) {
        validateEmail(userDto.getEmail());
        checkEmailIsFree(userDto.getEmail(), null);

        User user = new User(nextId++, userDto.getName(), userDto.getEmail());
        users.put(user.getId(), user);

        return UserMapper.toUserDto(user);
    }

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
        return users.values().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    public void delete(Long userId) {
        users.remove(userId);
    }

    public User getUser(Long userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return user;
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid email");
        }
    }

    private void checkEmailIsFree(String email, Long currentUserId) {
        boolean emailExists = users.values().stream()
                .anyMatch(user -> user.getEmail().equals(email) && !user.getId().equals(currentUserId));

        if (emailExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
    }
}
