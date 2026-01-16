#include "group_repository.h"
#include <fstream>
#include <iostream>
#include "../include/json.hpp"

using namespace std;
using json = nlohmann::json;

void GroupRepository::load(const string &filename)
{
    ifstream file(filename);
    if (!file.is_open())
    {
        cerr << "CRITICAL ERROR: Could not open group config file: " << filename << endl;
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
        Group group(key);

        // Safely load Size (default to 30 if missing)
        group.setSize(value.value("Size", 30));

        // Safely load Language
        // The error likely happened here or in Subjects if data was malformed
        if (value.contains("Language") && value["Language"].is_string())
        {
            group.setLanguage(value["Language"]);
        }
        else
        {
            group.setLanguage("English");
        }

        // --- THE FIX IS HERE ---
        // We strictly check if "Subjects" is an array of strings.
        if (value.contains("Subjects"))
        {
            const auto &subjectsJson = value["Subjects"];

            if (subjectsJson.is_array())
            {
                // Case A: It's a standard List ["Math", "Physics"]
                for (const auto &subj : subjectsJson)
                {
                    if (subj.is_string())
                    {
                        group.addSubject(subj.get<string>());
                    }
                }
            }
            else if (subjectsJson.is_object())
            {
                // Case B: It's an Object {"Math": {}, "Physics": {}} (Teacher format copy-paste?)
                // We handle this gracefully by just taking the keys.
                cout << "WARNING: Group " << key << " has 'Subjects' as an Object, expected Array. Attempting to parse keys..." << endl;
                for (auto &[subjName, val] : subjectsJson.items())
                {
                    group.addSubject(subjName);
                }
            }
            else
            {
                cerr << "ERROR: Group " << key << " has invalid 'Subjects' format." << endl;
            }
        }

        // Load Splits (Optional)
        group.setSeminarySplit(value.value("SeminarySplit", 1));
        group.setLaboratorySplit(value.value("LaboratorySplit", 1));

        groups.insert({key, group});
    }

    cout << "Successfully loaded " << groups.size() << " groups." << endl;
}

const Group &GroupRepository::getGroup(const string &id) const
{
    if (groups.find(id) == groups.end())
    {
        cerr << "ERROR: Group not found: " << id << endl;
        exit(1);
    }
    return groups.at(id);
}

const map<string, Group> &GroupRepository::getAll() const
{
    return groups;
}