#include "schedule_verifier.h"
#include <iostream>

using namespace std;

bool ScheduleVerifier::isRoomSuitable(const ClassSession &session, const Room &room, const string &buildingName)
{
    // Rule 1: Capacity
    // We don't have Group Size here directly, but we can assume the session will carry it
    // Or we check it in the solver. For now, let's assume if we picked the room, we checked capacity earlier.

    // Rule 2: Room Flags
    // If session is LABORATORY, room must NOT have "noLaboratory"
    if (session.type == ClassType::LABORATORY)
    {
        for (const auto &flag : room.flags)
        {
            if (flag == "noLaboratory")
                return false;
        }
    }

    // If session is SEMINARY, room must NOT have "noSeminar"
    if (session.type == ClassType::SEMINARY)
    {
        for (const auto &flag : room.flags)
        {
            if (flag == "noSeminar")
                return false;
        }
    }

    // If session is COURSE, room must NOT have "noCourse"
    if (session.type == ClassType::COURSE)
    {
        for (const auto &flag : room.flags)
        {
            if (flag == "noCourse")
                return false;
        }
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

        // Check Time Overlap
        if (TimeUtils::overlap(start, end, existing.startTime, existing.endTime))
        {

            // Conflict A: Same Room?
            if (existing.roomName == candidate.roomName)
                return false;

            // Conflict B: Same Teacher?
            // (Only if teacher is assigned)
            if (!candidate.teacherName.empty() && existing.teacherName == candidate.teacherName)
                return false;

            // Conflict C: Same Group?
            // "911" cannot be in two places.
            if (existing.groupId == candidate.groupId)
            {
                // EXCEPTION: Subgroups.
                // If 911/1 is in a Lab, 911/2 CAN be in a different Lab.
                // But 911 (Course) blocks 911/1 (Lab).

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