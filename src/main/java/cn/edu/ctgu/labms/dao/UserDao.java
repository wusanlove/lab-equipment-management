package cn.edu.ctgu.labms.dao;

import cn.edu.ctgu.labms.domain.Role;
import cn.edu.ctgu.labms.domain.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 用户数据访问对象（内存仓储实现）
 */
public class UserDao {
    private final Map<String, User> storage = new LinkedHashMap<>();
    private final AtomicInteger idSeq = new AtomicInteger(1000);

    public String nextId() {
        return "USR-" + idSeq.incrementAndGet();
    }

    public User save(User user) {
        storage.put(user.getId(), user);
        return user;
    }

    public Optional<User> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    public Optional<User> findByUsername(String username) {
        return storage.values().stream()
                .filter(u -> u.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public List<User> findByRole(Role role) {
        return storage.values().stream()
                .filter(u -> u.getRole() == role)
                .collect(Collectors.toList());
    }

    public List<User> findAll() {
        return new ArrayList<>(storage.values());
    }

    public boolean deleteById(String id) {
        return storage.remove(id) != null;
    }

    public void clear() {
        storage.clear();
        idSeq.set(1000);
    }
}
