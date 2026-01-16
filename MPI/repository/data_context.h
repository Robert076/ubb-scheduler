#pragma once
#include "subject_repository.h"
#include "teacher_repository.h"
#include "place_repository.h"
#include "group_repository.h"

struct DataContext
{
    SubjectRepository subjects;
    TeacherRepository teachers;
    PlaceRepository places;
    GroupRepository groups;

    // Helper to load everything at once
    void loadAll(const std::string &configFolder)
    {
        subjects.load(configFolder + "/subjects-config.json");
        teachers.load(configFolder + "/teachers-config.json");
        places.load(configFolder + "/places-config.json");
        groups.load(configFolder + "/groups-config.json");
    }
};