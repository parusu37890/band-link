package com.example.bandlink.service;

import com.example.bandlink.entity.*;
import com.example.bandlink.repository.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class ReleaseBlockUnitTest {
    final UserRepository users = mock(UserRepository.class);
    final BlockRepository blocks = mock(BlockRepository.class);
    final BlockService service = new BlockService(users, blocks, Clock.systemUTC());

    @Test void codeUt023_selfBlockCannotPersist() {
        assertThrows(IllegalArgumentException.class, () -> service.block(1L, 1L));
        verifyNoInteractions(users, blocks);
    }
    @Test void codeUt024_repeatedBlockDoesNotDuplicateRelationship() {
        when(users.findById(1L)).thenReturn(Optional.of(new User("A", "a@example.invalid", "hash")));
        when(users.findById(2L)).thenReturn(Optional.of(new User("B", "b@example.invalid", "hash")));
        when(blocks.existsByBlockerIdAndBlockedId(1L, 2L)).thenReturn(false, true);
        service.block(1L, 2L);
        service.block(1L, 2L);
        verify(blocks, times(1)).save(any(Block.class));
    }
    @Test void codeUt025_unblockDeletesOnlyOwnDirectionAndMissingBlockIsNoOp() {
        var relationship = new Block(new User("A", "a@example.invalid", "hash"),
                new User("B", "b@example.invalid", "hash"), LocalDateTime.now());
        when(blocks.findByBlockerIdAndBlockedId(1L, 2L)).thenReturn(Optional.of(relationship), Optional.empty());
        service.unblock(1L, 2L);
        service.unblock(1L, 2L);
        verify(blocks, times(1)).delete(relationship);
        verify(blocks, never()).findByBlockerIdAndBlockedId(2L, 1L);
    }
}
