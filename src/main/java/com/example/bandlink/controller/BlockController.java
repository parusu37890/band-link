package com.example.bandlink.controller;

import com.example.bandlink.dto.PeerResponse;
import com.example.bandlink.repository.UserRepository;
import com.example.bandlink.service.BlockService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/blocks")
public class BlockController {
    private final BlockService service;
    private final UserRepository users;
    public BlockController(BlockService service, UserRepository users) { this.service = service; this.users = users; }
    @GetMapping public List<PeerResponse> list(Authentication a) {
        return service.list(current(a)).stream().map(b -> PeerResponse.from(b.getBlocked())).toList();
    }
    @PostMapping public void block(Authentication a, @RequestParam Long userId) { service.block(current(a), userId); }
    @DeleteMapping("/{userId}") public void unblock(Authentication a, @PathVariable Long userId) { service.unblock(current(a), userId); }
    private Long current(Authentication a) { return users.findByEmail(a.getName()).orElseThrow().getId(); }
}
