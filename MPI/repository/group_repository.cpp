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

    // Iterate through Groups (e.g., "911", "931")
    for (auto &[groupId, details] : data.items())
    {
        Group group(groupId);

        int size = details.value("Size", 0);
        string language = details.value("Language", "English");
        int semSplit = details.value("SeminarySplit", 1);
        int labSplit = details.value("LaboratorySplit", 1);

        group.setDetails(size, language, semSplit, labSplit);

        // Parse Subjects list
        if (details.contains("Subjects"))
        {
            for (const auto &subjectName : details["Subjects"])
            {
                group.addSubject(subjectName);
            }
        }

        groups.insert({groupId, group});
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