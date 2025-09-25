package ru.izpz.edu.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.dto.model.WorkplaceV1DTO;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.model.Workplace;

@Mapper(componentModel = "spring")
public interface CampusMapper {
    @Mapping(source = "dto.id", target = "clusterId")
    @Mapping(source = "campusId", target = "campusId")
    Cluster toClusterEntity(ClusterV1DTO dto, String campusId);

    @Mapping(target = "id.clusterId", source = "clusterId")
    @Mapping(target = "id.row", source = "dto.row")
    @Mapping(target = "id.number", source = "dto.number")
    @Mapping(target = "login", source = "dto.login")
    Workplace toWorkplaceEntity(WorkplaceV1DTO dto, Long clusterId);
}
