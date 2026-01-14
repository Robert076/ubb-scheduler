#include "place_repository.h"
#include <fstream>
#include <iostream>
#include "../include/json.hpp"

using namespace std;
using json = nlohmann::json;

void PlaceRepository::load(const string &filename)
{
    ifstream file(filename);
    if (!file.is_open())
    {
        cerr << "CRITICAL ERROR: Could not open place config file: " << filename << endl;
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

    // Iterate through Buildings (e.g., "FSEGA", "CENTER")
    for (auto &[buildingName, details] : data.items())
    {
        Place place(buildingName);

        // 1. Parse Schedule (Building opening hours)
        if (details.contains("Schedule"))
        {
            for (auto &[day, intervals] : details["Schedule"].items())
            {
                for (const auto &interval : intervals)
                {
                    string start = interval.value("start", "00:00");
                    string end = interval.value("end", "00:00");
                    place.addSchedule(day, start, end);
                }
            }
        }

        // 2. Parse Rooms
        // "Rooms": { "C101": { "Capacity": 32, "Flags": ["noLaboratory"] } }
        if (details.contains("Rooms"))
        {
            for (auto &[roomName, roomData] : details["Rooms"].items())
            {
                int capacity = roomData.value("Capacity", 0);

                vector<string> flags;
                if (roomData.contains("Flags"))
                {
                    for (const auto &flag : roomData["Flags"])
                    {
                        flags.push_back(flag);
                    }
                }

                place.addRoom(roomName, capacity, flags);
            }
        }

        places.insert({buildingName, place});
    }

    cout << "Successfully loaded " << places.size() << " buildings/places." << endl;
}

const Place &PlaceRepository::getPlace(const string &name) const
{
    if (places.find(name) == places.end())
    {
        cerr << "ERROR: Place (Building) not found: " << name << endl;
        exit(1);
    }
    return places.at(name);
}

const map<string, Place> &PlaceRepository::getAll() const
{
    return places;
}