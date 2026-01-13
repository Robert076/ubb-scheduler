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
        exit(1); // Stop everything if config is missing
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

    // Iterate through the JSON object
    // Key = "Fundamentals of Programming", Value = { ...details... }
    for (auto &[key, value] : data.items())
    {
        string subjectName = key;
        Subject sub(subjectName);

        // 1. Basic Details
        // value.value("key", default) is safer than value["key"]
        sub.setDetails(
            value.value("MainTeacher", ""),
            value.value("Language", ""));

        // 2. Course Config
        // WATCH OUT: Matches the typo "Lenght" in your JSON
        sub.setCourseConfig(
            value.value("CoursesPerWeek", 0.0),
            value.value("CourseLenght", 0));

        // 3. Seminar Config
        sub.setSeminarConfig(
            value.value("SeminarsPerWeek", 0.0),
            value.value("SeminarLenght", 0));

        // 4. Laboratory Config
        sub.setLaboratoryConfig(
            value.value("LaboratoriesPerWeek", 0.0),
            value.value("LaboratoriesLenght", 0));

        // Add to our internal map
        subjects.insert({subjectName, sub});
    }

    cout << "Successfully loaded " << subjects.size() << " subjects." << endl;
}

const Subject &SubjectRepository::getSubject(const string &name) const
{
    if (subjects.find(name) == subjects.end())
    {
        cerr << "ERROR: Subject not found: " << name << endl;
        // In a real app we might throw, but for now we exit to be safe
        exit(1);
    }
    return subjects.at(name);
}

const map<string, Subject> &SubjectRepository::getAll() const
{
    return subjects;
}