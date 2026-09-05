package com.example.bandlink.controller;

import com.example.bandlink.dto.MasterOption;
import com.example.bandlink.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/masters")
public class MasterController {
    private final PartRepository parts;
    private final GenreRepository genres;
    private final StanceRepository stances;
    private final PrefectureRepository prefectures;
    public MasterController(PartRepository p, GenreRepository g, StanceRepository s, PrefectureRepository pr) {
        parts = p; genres = g; stances = s; prefectures = pr;
    }
    @GetMapping public Map<String, List<MasterOption>> list() {
        Sort order = Sort.by("displayOrder", "id");
        return Map.of("parts", parts.findAll(order).stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                "genres", genres.findAll(order).stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                "stances", stances.findAll(order).stream().map(p -> new MasterOption(p.getId(), p.getName())).toList(),
                "prefectures", prefectures.findAll(order).stream().map(p -> new MasterOption(p.getId(), p.getName())).toList());
    }
}
