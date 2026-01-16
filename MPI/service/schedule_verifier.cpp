#include "schedule_verifier.h"
#include <iostream>
using namespace std;

bool ScheduleVerifier::isRoomSuitable(const ClassSession &session, const Room &room, const string &buildingName)
{
    // Check room capabilities
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

bool ScheduleVerifier::isSlotFree(const DataContext &ctx,
                                  const vector<ClassSession> &currentSchedule,
                                  const ClassSession &candidate,
                                  const string &day,
                                  const string &start,
                                  const string &end)
{
    // Check teacher constraints
    if (!candidate.teacherName.empty())
    {
        if (ctx.teachers.getAll().count(candidate.teacherName))
        {
            const Teacher &teacher = ctx.teachers.getTeacher(candidate.teacherName);
            const auto &teacherSchedule = teacher.getSchedule();

            // Is teacher working today?
            if (teacherSchedule.find(day) == teacherSchedule.end())
            {
                return false;
            }

            // Check specific time window
            bool withinAvailability = false;
            for (const auto &interval : teacherSchedule.at(day))
            {
                if (TimeUtils::toMinutes(start) >= TimeUtils::toMinutes(interval.start) &&
                    TimeUtils::toMinutes(end) <= TimeUtils::toMinutes(interval.end))
                {
                    withinAvailability = true;
                    break;
                }
            }

            if (!withinAvailability)
            {
                return false;
            }
        }
    }

    // Check collisions with existing sessions
    for (const auto &existing : currentSchedule)
    {
        if (existing.day != day)
            continue;

        // Week parity check
        if ((existing.weekMask & candidate.weekMask) == 0)
            continue;

        // Time overlap check
        if (TimeUtils::overlap(start, end, existing.startTime, existing.endTime))
        {
            // Room occupied?
            if (existing.roomName == candidate.roomName)
                return false;

            // Teacher busy?
            if (!candidate.teacherName.empty() && existing.teacherName == candidate.teacherName)
                return false;

            // Group busy?
            if (existing.groupId == candidate.groupId)
            {
                bool existingIsWholeGroup = existing.subGroup.empty();
                bool candidateIsWholeGroup = candidate.subGroup.empty();

                // Conflict if either is the whole group
                if (existingIsWholeGroup || candidateIsWholeGroup)
                    return false;

                // Conflict if subgroups match
                if (existing.subGroup == candidate.subGroup)
                    return false;
            }
        }
    }
    return true;
}