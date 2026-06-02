package ru.practicum.shareit.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import ru.practicum.shareit.client.BaseClient;
import ru.practicum.shareit.user.dto.UserDto;

@Component
public class UserClient extends BaseClient {
    public UserClient(@Value("${shareit-server.url}") String serverUrl, RestTemplate restTemplate) {
        super(restTemplate, serverUrl + "/users");
    }

    public ResponseEntity<Object> create(UserDto userDto) {
        return post("", null, userDto);
    }

    public ResponseEntity<Object> update(Long userId, UserDto userDto) {
        return patch("/" + userId, null, userDto, java.util.Map.of());
    }

    public ResponseEntity<Object> getById(Long userId) {
        return get("/" + userId, null);
    }

    public ResponseEntity<Object> getAll() {
        return get("", null);
    }

    public ResponseEntity<Object> delete(Long userId) {
        return delete("/" + userId, null);
    }
}
