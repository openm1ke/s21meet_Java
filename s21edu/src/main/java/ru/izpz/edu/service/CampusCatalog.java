package ru.izpz.edu.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CampusCatalog {

  private static final Map<String, String> KNOWN_CAMPUSES = new LinkedHashMap<>();

  static {
    KNOWN_CAMPUSES.put("6bfe3c56-0211-4fe1-9e59-51616caac4dd", "MSK");
    KNOWN_CAMPUSES.put("7c293c9c-f28c-4b10-be29-560e4b000a34", "KZN");
    KNOWN_CAMPUSES.put("46e7d965-21e9-4936-bea9-f5ea0d1fddf2", "NSK");
    KNOWN_CAMPUSES.put("911fbc82-b821-4699-82ee-15184a5e6cef", "Surgut");
    KNOWN_CAMPUSES.put("a4a9fa63-63c0-4e81-a5ff-e1501d74362c", "Veliky Novgorod");
    KNOWN_CAMPUSES.put("5a23bec9-f989-485d-935b-3f0dc61c4812", "Nizhny Novgorod");
    KNOWN_CAMPUSES.put("07a3fa7e-2640-4330-a902-0752178df949", "Yakutsk");
    KNOWN_CAMPUSES.put("ebc4fada-4f32-4948-9ec6-8a2e73a3077a", "Lipetsk");
    KNOWN_CAMPUSES.put("e786cbfb-ed04-4e0e-8a01-6b0fa2256634", "Ufa");
    KNOWN_CAMPUSES.put("667a42af-5469-4a33-9858-677d9d20956a", "Samarkand");
    KNOWN_CAMPUSES.put("981e10b5-7553-406a-940e-83d34342c113", "Belgorod");
    KNOWN_CAMPUSES.put("4e77f0e8-7b08-4024-b7cc-c21542235995", "Omsk");
    KNOWN_CAMPUSES.put("04989f19-21a3-41c0-af4e-89db4d8d4c6b", "Sakhalin");
    KNOWN_CAMPUSES.put("c3809b97-5910-453c-b486-8fcc58a8cc32", "Magas");
    KNOWN_CAMPUSES.put("14b2cc80-bdce-4c71-b29e-3c1457d81130", "Volgograd");
    KNOWN_CAMPUSES.put("bad03b39-ffd4-4217-9d24-65535fe1f293", "Tashkent");
    KNOWN_CAMPUSES.put("52acbcb1-f203-401e-a013-ecd0e74fb531", "Sechenov");
    KNOWN_CAMPUSES.put("c1f23996-0661-4fa8-ae27-6e0fb707d73e", "Chelyabinsk");
    KNOWN_CAMPUSES.put("b4b36a53-d253-4840-9000-49061d74bf50", "Yaroslavl");
    KNOWN_CAMPUSES.put("6967bc67-955b-444a-8d8d-be8b1584374d", "Stavropol");
  }

  public List<String> targetCampusIds() {
    return List.copyOf(KNOWN_CAMPUSES.keySet());
  }

  public String campusName(String campusId) {
    return KNOWN_CAMPUSES.getOrDefault(campusId, "UNKNOWN");
  }
}
