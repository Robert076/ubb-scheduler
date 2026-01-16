#pragma once
#include <map>
#include <string>
#include <vector>
#include "../model/subject.h"

class SubjectRepository
{
private:
    // Map: Subject Name -> Subject Object
    std::map<std::string, Subject> subjects;

public:
    // Loads the subjects-config.json file
    void load(const std::string &filename);

    // Get a specific subject by name
    const Subject &getSubject(const std::string &name) const;

    // Get all subjects (useful for iterating later)
    const std::map<std::string, Subject> &getAll() const;
};