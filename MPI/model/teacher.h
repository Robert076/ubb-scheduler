#include <string>
#include <vector>
#include <map>
#include "time_interval.h"

using namespace std;

struct TeachingAbility
{
    bool canSeminary;
    bool canLaboratory;
};

class Teacher
{
private:
    string name;
    int maxHoursPerWeek;
    vector<string> preferredBuildings;
    vector<string> languages;

    // Subject Name -> Ability (can do lab? can do seminar?)
    map<string, TeachingAbility> capableSubjects;

    // Day -> [] of time intervals
    map<string, vector<TimeInterval>> schedule;

public:
    Teacher(string name) : name(name) {}

    // Methods to populate data
    void addSchedule(const string &day, const string &start, const string &end)
    {
        schedule[day].push_back({start, end});
    }

    void addCapableSubject(const string &subjectName, bool canSeminary, bool canLaboratory)
    {
        capableSubjects[subjectName] = {canSeminary, canLaboratory};
    }

    void setPreferences(int maxHours, const vector<string> &buildings, const vector<string> &langs)
    {
        maxHoursPerWeek = maxHours;
        preferredBuildings = buildings;
        languages = langs;
    }

    // Getters
    string getName() const { return name; }
    int getMaxHours() const { return maxHoursPerWeek; }
    const vector<string> &getPreferredBuildings() const { return preferredBuildings; }
    const map<string, TeachingAbility> &getCapableSubjects() const { return capableSubjects; }
    const map<string, vector<TimeInterval>> &getSchedule() const { return schedule; }
};