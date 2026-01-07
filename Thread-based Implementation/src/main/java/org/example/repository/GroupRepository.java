package org.example.repository;

import org.example.model.Group;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GroupRepository {
    private Map<String, Group> groups = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    public GroupRepository() throws Exception {
        loadGroups();
    }

    private void loadGroups() throws Exception {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("groups-config.json");
        
        if (inputStream == null) {
            throw new RuntimeException("groups-config.json not found in resources!");
        }

        // Parse JSON as Map<String, Group>
        Map<String, Group> parsedGroups = objectMapper.readValue(inputStream,
            objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Group.class));

        // Set id for each group
        parsedGroups.forEach((id, group) -> group.setId(id));
        
        this.groups = parsedGroups;
    }

    public Group getGroup(String id) {
        return groups.get(id);
    }

    public Map<String, Group> getAllGroups() {
        return new HashMap<>(groups);
    }

    public void printAllGroups() {
        System.out.println("\n=== ALL GROUPS ===");
        groups.forEach((id, group) -> {
            System.out.println("\nGroup: " + id);
            System.out.println("  Size: " + group.getSize());
            System.out.println("  Language: " + group.getLanguage());
            System.out.println("  Subjects: " + group.getSubjects());
            System.out.println("  Seminar split: " + group.getSeminarySplit() + " (group size: " + group.getSeminaryGroupSize() + ")");
            System.out.println("  Laboratory split: " + group.getLaboratorySplit() + " (group size: " + group.getLaboratoryGroupSize() + ")");
        });
    }
}