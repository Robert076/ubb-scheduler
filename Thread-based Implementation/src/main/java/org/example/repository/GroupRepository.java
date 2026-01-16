package org.example.repository;

import org.example.model.Group;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;

/**
 * Repository for loading and managing Group entities from configuration.
 * Data is loaded once and exposed as unmodifiable collections.
 */
public class GroupRepository {

    private final Map<String, Group> groups;

    public GroupRepository() throws Exception {
        this.groups = Collections.unmodifiableMap(loadGroups());
    }

    private Map<String, Group> loadGroups() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("groups-config.json");

        if (inputStream == null) {
            throw new RuntimeException("groups-config.json not found in resources!");
        }

        Map<String, Group> parsedGroups = objectMapper.readValue(inputStream,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Group.class));

        // Set ID for each group (from JSON key)
        parsedGroups.forEach((id, group) -> group.setId(id));

        return parsedGroups;
    }

    /**
     * Retrieve a group by its ID.
     */
    public Group getGroup(String id) {
        return groups.get(id);
    }

    /**
     * Get all groups as an unmodifiable map.
     */
    public Map<String, Group> getAllGroups() {
        return groups;
    }

    /**
     * Get all group IDs.
     */
    public Set<String> getAllGroupIds() {
        return groups.keySet();
    }

    /**
     * Check if a group exists by ID.
     */
    public boolean existsGroup(String id) {
        return groups.containsKey(id);
    }

    /**
     * Get total number of groups.
     */
    public int getGroupCount() {
        return groups.size();
    }

    /**
     * Print all groups for debugging purposes.
     */
    public void printAllGroups() {
        System.out.println("\n=== ALL GROUPS ===");
        groups.forEach((id, group) -> {
            System.out.println("\nGroup: " + id);
            System.out.println(" Size: " + group.getSize());
            System.out.println(" Language: " + group.getLanguage());
            System.out.println(" Subjects: " + group.getSubjects());
            System.out.println(" Seminar split: " + group.getSeminarySplit() + " (group size: " + group.getSeminaryGroupSize() + ")");
            System.out.println(" Laboratory split: " + group.getLaboratorySplit() + " (group size: " + group.getLaboratoryGroupSize() + ")");
        });
    }
}
