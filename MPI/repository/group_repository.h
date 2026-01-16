#pragma once
#include <map>
#include <string>
#include <vector>
#include "../model/group.h"

class GroupRepository
{
private:
    // Map: Group ID ("911") -> Group Object
    std::map<std::string, Group> groups;

public:
    void load(const std::string &filename);

    // Get a specific group by ID
    const Group &getGroup(const std::string &id) const;

    // Get all groups
    const std::map<std::string, Group> &getAll() const;
};