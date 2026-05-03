package ru.practicum.ewm.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;

    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        PageRequest page = PageRequest.of(from / size, size);
        List<User> users;
        if (ids == null || ids.isEmpty()) {
            users = repository.findAll(page).getContent();
        } else {
            users = repository.findAllByIdIn(ids, page);
        }
        return users.stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Transactional
    public UserDto createUser(NewUserRequest dto) {
        if (repository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Email " + dto.getEmail() + " is already in use.");
        }
        User user = repository.save(UserMapper.toUser(dto));
        return UserMapper.toUserDto(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!repository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found.");
        }
        repository.deleteById(userId);
    }
}