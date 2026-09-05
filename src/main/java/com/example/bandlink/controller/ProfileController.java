package com.example.bandlink.controller;
import com.example.bandlink.dto.*; import com.example.bandlink.entity.User; import com.example.bandlink.repository.UserRepository; import com.example.bandlink.service.ProfileService; import jakarta.validation.Valid; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/users") public class ProfileController {
 private final ProfileService service; private final UserRepository users;
 public ProfileController(ProfileService s,UserRepository u){service=s;users=u;}
 @GetMapping("/me") public UserResponse me(Authentication a){return UserResponse.from(current(a));}
 @PutMapping("/me") public UserResponse update(Authentication a,@Valid @RequestBody ProfileUpdateRequest r){return UserResponse.from(service.update(current(a).getId(),r));}
 @GetMapping("/{id}") public UserResponse profile(@PathVariable Long id){return UserResponse.from(service.getPublic(id));}
 private User current(Authentication a){return users.findByEmail(a.getName()).orElseThrow(()->new IllegalStateException("認証ユーザーが見つかりません"));}
}
