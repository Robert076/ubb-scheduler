#pragma once
#include "../repository/data_context.h"
#include "../model/class_session.h"
#include "../utils/time_utils.h"

class ScheduleVerifier
{
public:
    static bool isRoomSuitable(const ClassSession &session, const Room &room, const std::string &buildingName);

    static bool isSlotFree(const DataContext &ctx,
                           const std::vector<ClassSession> &currentSchedule,
                           const ClassSession &candidate,
                           const std::string &day,
                           const std::string &start,
                           const std::string &end);
};