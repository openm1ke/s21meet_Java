package ru.izpz.edu.mapper;

import org.junit.jupiter.api.Test;
import ru.izpz.dto.model.ClusterV1DTO;
import ru.izpz.dto.model.WorkplaceV1DTO;
import ru.izpz.edu.model.Cluster;
import ru.izpz.edu.model.Workplace;
import ru.izpz.edu.service.GraphQLService;

import static org.junit.jupiter.api.Assertions.*;

class CampusMapperTest {

    private final CampusMapper mapper = org.mapstruct.factory.Mappers.getMapper(CampusMapper.class);

    @Test
    void toClusterEntity_shouldMapFields() {
        ClusterV1DTO dto = new ClusterV1DTO();
        dto.setId(42L);
        dto.setName("c");
        dto.setCapacity(10);
        dto.setAvailableCapacity(5);
        dto.setFloor(2);

        Cluster entity = mapper.toClusterEntity(dto, "campus123");

        assertEquals(42L, entity.getClusterId());
        assertEquals("c", entity.getName());
        assertEquals(10, entity.getCapacity());
        assertEquals(5, entity.getAvailableCapacity());
        assertEquals(2, entity.getFloor());
        assertEquals("campus123", entity.getCampusId());
    }

    @Test
    void toWorkplaceEntity_shouldMapFields() {
        WorkplaceV1DTO dto = new WorkplaceV1DTO();
        dto.setRow("A");
        dto.setNumber(1);
        dto.setLogin("login");

        Workplace entity = mapper.toWorkplaceEntity(dto, 123L);

        assertEquals(123L, entity.getId().getClusterId());
        assertEquals("A", entity.getId().getRow());
        assertEquals(1, entity.getId().getNumber());
        assertEquals("login", entity.getLogin());
    }

    @Test
    void toWorkplaceEntityV2_shouldMapFields() {
        GraphQLService.ClusterSeat seat = new GraphQLService.ClusterSeat("1", "A", 1, "login", 10, 2, "sg", "sn");

        Workplace entity = mapper.toWorkplaceEntityV2(seat, 123L);

        assertEquals(123L, entity.getId().getClusterId());
        assertEquals("A", entity.getId().getRow());
        assertEquals(1, entity.getId().getNumber());
        assertEquals("login", entity.getLogin());
        assertEquals(10, entity.getExpValue());
        assertEquals(2, entity.getLevelCode());
        assertEquals("sg", entity.getStageGroupName());
        assertEquals("sn", entity.getStageName());
    }
}
