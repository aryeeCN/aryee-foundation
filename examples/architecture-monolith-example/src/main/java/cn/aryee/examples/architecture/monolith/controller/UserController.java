package cn.aryee.examples.architecture.monolith.controller;

import cn.aryee.examples.architecture.monolith.entity.User;
import cn.aryee.examples.architecture.monolith.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户管理 REST API（单体架构示例）
 * <p>
 * 演示在单体架构下使用 JPA + Caffeine 缓存 + 本地事务的完整 CRUD。
 *
 * @author Aryee
 * @since 1.2.0
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 创建用户（本地事务 + Caffeine 缓存自动失效）
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        user.setId(null);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * 获取用户（Caffeine 缓存加速）
     */
    @GetMapping("/{id}")
    @Cacheable(value = "users", key = "#id")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取所有用户
     */
    @GetMapping
    public List<User> listUsers() {
        return userRepository.findAll();
    }

    /**
     * 更新用户（本地事务 + 缓存自动失效）
     */
    @PutMapping("/{id}")
    @CacheEvict(value = "users", key = "#id")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return userRepository.findById(id)
                .map(existing -> {
                    existing.setUsername(user.getUsername());
                    existing.setEmail(user.getEmail());
                    existing.setAge(user.getAge());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return ResponseEntity.ok(userRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 删除用户（缓存自动失效）
     */
    @DeleteMapping("/{id}")
    @CacheEvict(value = "users", key = "#id")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}