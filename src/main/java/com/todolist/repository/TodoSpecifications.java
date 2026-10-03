package com.todolist.repository;

import com.todolist.entity.Todo;
import com.todolist.enums.TodoStatus;
import org.springframework.data.jpa.domain.Specification;

public class TodoSpecifications {

    private TodoSpecifications() {}

    public static Specification<Todo> forUser(Long userId, String keyword, TodoStatus status) {
        return (root, query, cb) -> {
            var predicate = cb.equal(root.get("user").get("userId"), userId);

            if (keyword != null && !keyword.isBlank()) {
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%"));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            return predicate;
        };
    }
}
