package com.legal.user;

import com.legal.user.dto.*;
import com.legal.user.exception.UserNotFoundException;
import com.legal.user.mapper.UserMapper;
import com.legal.user.model.*;
import com.legal.user.repository.*;
import com.legal.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).name("Test User").username("testuser")
                .email("test@example.com").status(User.UserStatus.ACTIVE)
                .roles(new HashSet<>()).categories(new HashSet<>())
                .build();

        testUserDTO = UserDTO.builder()
                .id(1L).name("Test User").username("testuser")
                .email("test@example.com").status(User.UserStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Obtener usuario por ID exitosamente")
    void getUserById_Found_ReturnsDTO() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);

        UserDTO result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Obtener usuario inexistente lanza excepción")
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Crear usuario exitosamente")
    void createUser_Success() {
        CreateUserRequest request = CreateUserRequest.builder()
                .name("Nuevo").username("nuevo").email("nuevo@test.com").build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userMapper.toEntity(any(CreateUserRequest.class))).thenReturn(testUser);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(new Role()));
        when(userRepository.save(any())).thenReturn(testUser);
        when(userMapper.toDTO(any())).thenReturn(testUserDTO);

        UserDTO result = userService.createUser(request, 1L);

        assertThat(result).isNotNull();
        verify(userRepository).save(any());
    }

    @Test
    @DisplayName("Crear usuario falla si email duplicado")
    void createUser_DuplicateEmail_ThrowsException() {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("test@example.com").username("newuser").build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("Actualizar estado del usuario")
    void updateStatus_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(any())).thenReturn(testUserDTO);

        userService.updateStatus(1L, User.UserStatus.INACTIVE);

        verify(userRepository).updateStatus(1L, User.UserStatus.INACTIVE);
    }

    @Test
    @DisplayName("Eliminar usuario existente")
    void deleteUser_Success() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Eliminar usuario inexistente lanza excepción")
    void deleteUser_NotFound_ThrowsException() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteUser(999L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Estadísticas de usuarios")
    void getUserStats_ReturnsMap() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByStatus(User.UserStatus.ACTIVE)).thenReturn(40L);
        when(userRepository.countByStatus(User.UserStatus.INACTIVE)).thenReturn(5L);
        when(userRepository.countByStatus(User.UserStatus.SUSPENDED)).thenReturn(3L);
        when(userRepository.countByStatus(User.UserStatus.PENDING_VERIFICATION)).thenReturn(2L);

        Map<String, Long> stats = userService.getUserStats();

        assertThat(stats).containsKeys("total", "active", "inactive", "suspended", "pending");
        assertThat(stats.get("total")).isEqualTo(50L);
        assertThat(stats.get("active")).isEqualTo(40L);
    }
}
