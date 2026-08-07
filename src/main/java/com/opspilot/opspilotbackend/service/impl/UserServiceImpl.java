package com.opspilot.opspilotbackend.service.impl;

import com.opspilot.opspilotbackend.dto.UserRequestDto;
import com.opspilot.opspilotbackend.dto.UserResponseDto;
import com.opspilot.opspilotbackend.entity.User;
import com.opspilot.opspilotbackend.mapper.UserMapper;
import com.opspilot.opspilotbackend.repository.UserRepository;
import com.opspilot.opspilotbackend.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponseDto createUser(UserRequestDto request) {

        User user = UserMapper.toEntity(request);

        user = userRepository.save(user);

        return UserMapper.toResponseDto(user);
    }

    @Override
    public List<UserResponseDto> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(UserMapper::toResponseDto)
                .toList();
    }

    @Override
    public UserResponseDto getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return UserMapper.toResponseDto(user);
    }

    @Override
    public UserResponseDto updateUser(Long id, UserRequestDto request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setRole(request.getRole());
        user.setCompanyId(request.getCompanyId());
        user.setActive(request.isActive());

        user = userRepository.save(user);

        return UserMapper.toResponseDto(user);
    }

    @Override
    public void deleteUser(Long id) {

        userRepository.deleteById(id);
    }
}