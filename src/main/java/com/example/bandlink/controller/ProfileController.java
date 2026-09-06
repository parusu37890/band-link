package com.example.bandlink.controller;

import com.example.bandlink.dto.*;
import com.example.bandlink.entity.User;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.service.ImageStorageService;
import com.example.bandlink.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @RequestMapping("/api/users")
public class ProfileController {
    private final ProfileService service; private final UserRepository users; private final ImageStorageService storage;
    public ProfileController(ProfileService service,UserRepository users,ImageStorageService storage){this.service=service;this.users=users;this.storage=storage;}
    @GetMapping("/me") public MyProfileResponse me(Authentication a){return MyProfileResponse.from(current(a));}
    @PutMapping("/me") public MyProfileResponse update(Authentication a,@Valid @RequestBody ProfileUpdateRequest r){return MyProfileResponse.from(service.update(current(a).getId(),r));}
    @PostMapping("/me/image") public MyProfileResponse image(Authentication a,@RequestParam MultipartFile file){User user=current(a);String previous=user.getProfileImageUrl();String next=storage.store(file);user.setProfileImageUrl(next);users.save(user);if(previous!=null)storage.delete(previous);return MyProfileResponse.from(user);}
    @DeleteMapping("/me/image") public void removeImage(Authentication a){User user=current(a);storage.delete(user.getProfileImageUrl());user.setProfileImageUrl(null);users.save(user);}
    @GetMapping("/{id}") public ProfileResponse profile(@PathVariable Long id){return ProfileResponse.from(service.getPublic(id));}
    private User current(Authentication a){return users.findByEmail(a.getName()).orElseThrow(()->new IllegalStateException("ユーザーが見つかりません"));}
}
