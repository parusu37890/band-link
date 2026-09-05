package com.example.bandlink.controller;
import com.example.bandlink.dto.SearchHistoryResponse;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.service.SearchHistoryService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/search-history")
public class SearchHistoryController {
    private final SearchHistoryService service; private final UserRepository users;
    public SearchHistoryController(SearchHistoryService service, UserRepository users) { this.service = service; this.users = users; }
    @GetMapping public List<SearchHistoryResponse> list(Authentication authentication) {
        Long id = users.findByEmail(authentication.getName()).orElseThrow(() -> new IllegalStateException("認証ユーザーが見つかりません")).getId();
        return service.recent(id).stream().map(SearchHistoryResponse::from).toList();
    }
}
