package org.example.service.generation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.model.Activity;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ScheduleExporter {

    private static final String BASE_FILENAME = "schedule_output";
    private static final String EXTENSION = ".json";

    public void export(List<Activity> activities) throws IOException {
        // Prepare data for export in the requested format
        List<Map<String, Object>> exportData = activities.stream()
                .filter(a -> !a.groupId().equals("ALL_GROUPS")) // Filter out internal teacher activities
                .map(this::mapActivityToExport)
                .collect(Collectors.toList());

        String filename = findNextAvailableFilename();
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        mapper.writeValue(new File(filename), exportData);
        System.out.println("\n[EXPORT] Schedule exported to: " + filename);
    }

    private Map<String, Object> mapActivityToExport(Activity activity) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("day", capitalize(activity.day()));
        map.put("start", activity.startTime().toString());
        map.put("end", activity.endTime().toString());
        map.put("type", capitalize(activity.activityType()));
        map.put("group", activity.groupId());
        map.put("subgroup", activity.subgroup());
        map.put("subject", activity.subjectName());
        map.put("teacher", activity.teacherName());
        map.put("room", activity.roomId());
        map.put("frequency", activity.frequency());
        return map;
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String findNextAvailableFilename() {
        File file = new File(BASE_FILENAME + EXTENSION);
        if (!file.exists()) {
            return BASE_FILENAME + EXTENSION;
        }

        int counter = 2;
        while (true) {
            String filename = BASE_FILENAME + "_" + counter + EXTENSION;
            file = new File(filename);
            if (!file.exists()) {
                return filename;
            }
            counter++;
        }
    }
}
