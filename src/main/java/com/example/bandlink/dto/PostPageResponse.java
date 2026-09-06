package com.example.bandlink.dto;

import java.util.List;

public record PostPageResponse(List<PostResponse> items, String nextCursor, boolean hasNext) {}
