package com.example.bandlink.config;

import com.example.bandlink.entity.*;
import com.example.bandlink.repository.*;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class MasterDataInitializerTest {
    @Test void seedPreservesExistingOptionsAndIsIdempotent() throws Exception {
        var parts = mock(PartRepository.class); var genres = mock(GenreRepository.class);
        var stances = mock(StanceRepository.class); var prefectures = mock(PrefectureRepository.class);
        var p = new ArrayList<Part>(); p.add(new Part("ギター", 20)); p.add(new Part("ユーザー追加", 99));
        var g = new ArrayList<Genre>(); var s = new ArrayList<Stance>(); var pr = new ArrayList<Prefecture>();
        when(parts.findAll()).thenReturn(p); when(genres.findAll()).thenReturn(g);
        when(stances.findAll()).thenReturn(s); when(prefectures.findAll()).thenReturn(pr);
        when(parts.save(any())).thenAnswer(i -> { Part v = i.getArgument(0); p.add(v); return v; });
        when(genres.save(any())).thenAnswer(i -> { Genre v = i.getArgument(0); g.add(v); return v; });
        when(stances.save(any())).thenAnswer(i -> { Stance v = i.getArgument(0); s.add(v); return v; });
        when(prefectures.save(any())).thenAnswer(i -> { Prefecture v = i.getArgument(0); pr.add(v); return v; });
        var initializer = new MasterDataInitializer(parts, genres, stances, prefectures);
        initializer.run(null); initializer.run(null);
        assertEquals(11, p.size()); assertEquals(14, g.size()); assertEquals(4, s.size()); assertEquals(47, pr.size());
        assertEquals(20, p.getFirst().getDisplayOrder());
        verify(parts, times(9)).save(any()); verify(prefectures, times(47)).save(any());
    }
}
