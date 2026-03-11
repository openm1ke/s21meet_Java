package ru.izpz.edu.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.izpz.dto.FriendDto;
import ru.izpz.edu.model.Friends;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FriendsMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "telegramId", source = "entity.telegramId")
    @Mapping(target = "name", source = "entity.name")
    @Mapping(target = "login", source = "entity.login")
    @Mapping(target = "isFriend", source = "entity.isFriend")
    @Mapping(target = "isSubscribe", source = "entity.isSubscribe")
    @Mapping(target = "isFavorite", source = "entity.isFavorite")
    FriendDto toDto(Friends entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "date", ignore = true)
    Friends toEntity(FriendDto dto);

    List<FriendDto> toDtos(List<Friends> entities);
}
