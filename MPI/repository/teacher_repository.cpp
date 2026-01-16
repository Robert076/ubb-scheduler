#include "teacher_repository.h"
#include <fstream>
#include <iostream>
#include "../include/json.hpp" // Adjust path if needed

using namespace std;
using json = nlohmann::json;

void TeacherRepository::load(const string &filename)
{
    ifstream file(filename);
    if (!file.is_open())
    {
        cerr << "CRITICAL ERROR: Could not open teacher config file: " << filename << endl;
        exit(1);
    }

    json data;
    try
    {
        file >> data;
    }
    catch (const json::parse_error &e)
    {
        cerr << "CRITICAL ERROR: JSON parsing failed for " << filename << ": " << e.what() << endl;
        exit(1);
    }

    // Iterate through teachers (e.g., "Gabriel Mircea")
    for (auto &[teacherName, details] : data.items())
    {
        Teacher teacher(teacherName);

        // 1. Basic Preferences
        int maxHours = details.value("MaxHoursPerWeek", 40);

        vector<string> buildings;
        if (details.contains("PreferredBuildings"))
        {
            for (const auto &b : details["PreferredBuildings"])
            {
                buildings.push_back(b);
            }
        }

        vector<string> languages;
        if (details.contains("Languages"))
        {
            for (const auto &l : details["Languages"])
            {
                languages.push_back(l);
            }
        }

        teacher.setPreferences(maxHours, buildings, languages);

        // 2. Capabilities (Subjects)
        // "Subjects": { "Fundamentals of Programming": { "canSeminary": true, ... } }
        if (details.contains("Subjects"))
        {
            for (auto &[subName, caps] : details["Subjects"].items())
            {
                bool canSem = caps.value("canSeminary", false);
                bool canLab = caps.value("canLaboratory", false);
                teacher.addCapableSubject(subName, canSem, canLab);
            }
        }

        // 3. Schedule
        // "Schedule": { "Monday": [ {"start": "08:00", "end": "12:00"} ] }
        if (details.contains("Schedule"))
        {
            for (auto &[day, intervals] : details["Schedule"].items())
            {
                for (const auto &interval : intervals)
                {
                    string start = interval.value("start", "00:00");
                    string end = interval.value("end", "00:00");
                    teacher.addSchedule(day, start, end);
                }
            }
        }

        teachers.insert({teacherName, teacher});
    }

    cout << "Successfully loaded " << teachers.size() << " teachers." << endl;
}

const Teacher &TeacherRepository::getTeacher(const string &name) const
{
    if (teachers.find(name) == teachers.end())
    {
        cerr << "ERROR: Teacher not found: " << name << endl;
        exit(1);
    }
    return teachers.at(name);
}

const map<string, Teacher> &TeacherRepository::getAll() const
{
    return teachers;
}