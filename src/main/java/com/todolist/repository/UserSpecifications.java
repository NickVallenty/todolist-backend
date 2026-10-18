package com.todolist.repository;

import com.todolist.entity.AppUser;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

    private UserSpecifications() {}

    /** Tìm user có username HOẶC email chứa keyword (không phân biệt hoa thường). Keyword trống = lấy tất cả. */
    public static Specification<AppUser> search(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            String escaped = keyword.trim().toLowerCase()
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_");
            String like = "%" + escaped + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("username")), like, '!'),
                    cb.like(cb.lower(root.get("email")), like, '!')
            );
        };
    }
}