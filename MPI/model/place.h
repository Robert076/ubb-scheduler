#include <string>
#include <vector>
#include <map>
#include "time_interval.h"

using namespace std;

struct Room
{
    int capacity;
    vector<string> flags;
};

class Place
{
private:
    string name;

    // day -> [] of time intervals
    map<string, vector<TimeInterval>> schedule;

    // name -> room
    map<string, Room> rooms;

public:
    Place(string name) : name(name) {}

    void addSchedule(const string &day, const string &start, const string &end)
    {
        schedule[day].push_back({start, end});
    }

    void addRoom(const string &roomName, int capacity, const vector<string> &flags)
    {
        rooms[roomName] = {capacity, flags};
    }

    string getName() const { return name; }
    const map<string, Room> &getRooms() const { return rooms; }
    const map<string, vector<TimeInterval>> &getSchedule() const { return schedule; }
};