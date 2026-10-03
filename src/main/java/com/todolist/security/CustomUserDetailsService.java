package com.todolist.security;

import com.todolist.entity.AppUser;
import com.todolist.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service   // khai báo class này là nơi thực hiện nghiệp vụ, giúp chỗ khác có thể gọi nó để thực hiện nghiệp vụ
@RequiredArgsConstructor   // sinh ra constructure trong lúc biên dịch không cần phải tự constructure
public class CustomUserDetailsService implements UserDetailsService {


    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user: " + username));
        return new CustomUserDetails(appUser);
    }
}
