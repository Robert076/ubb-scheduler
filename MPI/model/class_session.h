#pragma once
#include <string>

enum ClassType
{
    COURSE,
    SEMINARY,
    LABORATORY
};

struct ClassSession
{
    // Who and What
    std::string subjectName;
    ClassType type;       // Is it a Course, Lab, or Seminary?
    std::string groupId;  // "911"
    std::string subGroup; // "1" or "2" (for labs/seminars), or "" for courses

    // The Resource Assignment (This is what the algo tries to fill)
    std::string teacherName;
    std::string buildingName;
    std::string roomName;

    // Time
    std::string day;
    std::string startTime;
    std::string endTime;

    int weekMask = 3;

    std::string toString() const
    {
        std::string weekStr = (weekMask == 3) ? "" : (weekMask == 1 ? "[Odd]" : "[Even]");
        return weekStr + "[" + day + " " + startTime + "] ...";
    }
};