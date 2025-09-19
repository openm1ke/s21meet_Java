package ru.izpz.dto;

import java.util.List;

public record FriendsSliceDto(
        List<FriendDto> content,
        int page,
        int size,
        boolean hasNext
) {}
