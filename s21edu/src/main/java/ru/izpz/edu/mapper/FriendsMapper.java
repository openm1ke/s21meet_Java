package ru.izpz.edu.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.izpz.dto.FriendDto;
import ru.izpz.edu.model.Friends;

@Mapper(componentModel = "spring")
public interface FriendsMapper {

    FriendDto toDto(Friends entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "date", ignore = true)
    Friends toEntity(FriendDto dto);
}
