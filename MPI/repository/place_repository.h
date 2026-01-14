#pragma once
#include <map>
#include <string>
#include <vector>
#include "../model/place.h"

class PlaceRepository
{
private:
    // Map: Building Name ("FSEGA") -> Place Object
    std::map<std::string, Place> places;

public:
    void load(const std::string &filename);

    // Get a building by name
    const Place &getPlace(const std::string &name) const;

    // Get all buildings
    const std::map<std::string, Place> &getAll() const;
};