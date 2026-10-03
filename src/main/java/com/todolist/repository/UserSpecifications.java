package com.todolist.repository;

import com.todolist.entity.AppUser;
import com.todolist.enums.Role;
import com.todolist.enums.UserStatus;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

    private UserSpecifications() {}

    public static Specification<AppUser> search(String keyword, Role role, UserStatus status) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("username")), like),
                        cb.like(cb.lower(root.get("email")), like)
                ));
            }
            if (role != null) {
                predicate = cb.and(predicate, cb.equal(root.get("role"), role));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            return predicate;
        };
    }
}
