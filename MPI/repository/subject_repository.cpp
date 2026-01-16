#include "subject_repository.h"
#include <fstream>
#include <iostream>
#include "../include/json.hpp"

using namespace std;
using json = nlohmann::json;

void SubjectRepository::load(const string &filename)
{
    ifstream file(filename);
    if (!file.is_open())
    {
        cerr << "CRITICAL ERROR: Could not open subject config file: " << filename << endl;
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

    for (auto &[key, value] : data.items())
    {
        string subjectName = key;
        Subject sub(subjectName);

        // --- FIX: Check both "MainTeacher" and "Main Teacher" ---
        string teacher = value.value("MainTeacher", "");
        if (teacher.empty())
        {
            teacher = value.value("Main Teacher", "");
        }
        sub.setMainTeacher(teacher);
        // --------------------------------------------------------

        sub.setLanguage(value.value("Language", ""));

        // Course Config
        sub.setCourseConfig(
            value.value("CoursesPerWeek", 0.0),
            value.value("CourseLength", 0));

        // Seminar Config
        sub.setSeminarConfig(
            value.value("SeminarsPerWeek", 0.0),
            value.value("SeminarLength", 0));

        // Laboratory Config
        sub.setLaboratoryConfig(
            value.value("LaboratoriesPerWeek", 0.0),
            value.value("LaboratoriesLength", 0));

        // Lab Split Override
        sub.setLabSplitOverride(value.value("LabSplit", 0));

        subjects.insert({subjectName, sub});
    }

    cout << "Successfully loaded " << subjects.size() << " subjects." << endl;
}

// ... keep getSubject and getAll as they were ...
const Subject &SubjectRepository::getSubject(const string &name) const
{
    if (subjects.find(name) == subjects.end())
    {
        cerr << "ERROR: Subject not found: " << name << endl;
        exit(1);
    }
    return subjects.at(name);
}

const map<string, Subject> &SubjectRepository::getAll() const
{
    return subjects;
}