#include "schedule_verifier.h"
#include <iostream>

using namespace std;

bool ScheduleVerifier::isRoomSuitable(const ClassSession &session, const Room &room, const string &buildingName)
{
    // Rule 1: Room Flags
    if (session.type == ClassType::LABORATORY)
    {
        for (const auto &flag : room.flags)
            if (flag == "noLaboratory")
                return false;
    }
    if (session.type == ClassType::SEMINARY)
    {
        for (const auto &flag : room.flags)
            if (flag == "noSeminar")
                return false;
    }
    if (session.type == ClassType::COURSE)
    {
        for (const auto &flag : room.flags)
            if (flag == "noCourse")
                return false;
    }
    return true;
}

bool ScheduleVerifier::isSlotFree(const vector<ClassSession> &currentSchedule,
                                  const ClassSession &candidate,
                                  const string &day,
                                  const string &start,
                                  const string &end)
{

    // Iterate over everything already scheduled
    for (const auto &existing : currentSchedule)
    {
        // Optimization: Different days don't conflict
        if (existing.day != day)
            continue;

        // NEW: Check Week Mask Overlap
        // If (Odd & Even) -> 0, they do NOT conflict.
        if ((existing.weekMask & candidate.weekMask) == 0)
            continue;

        // Check Time Overlap
        if (TimeUtils::overlap(start, end, existing.startTime, existing.endTime))
        {

            // Conflict A: Same Room?
            if (existing.roomName == candidate.roomName)
                return false;

            // Conflict B: Same Teacher?
            if (!candidate.teacherName.empty() && existing.teacherName == candidate.teacherName)
                return false;

            // Conflict C: Same Group?
            if (existing.groupId == candidate.groupId)
            {

                bool existingIsWholeGroup = existing.subGroup.empty();
                bool candidateIsWholeGroup = candidate.subGroup.empty();

                // If either is the whole group, they conflict
                if (existingIsWholeGroup || candidateIsWholeGroup)
                    return false;

                // If both are subgroups, they only conflict if they are the SAME subgroup
                if (existing.subGroup == candidate.subGroup)
                    return false;
            }
        }
    }

    return true;
}