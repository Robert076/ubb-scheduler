#pragma once
#include "../repository/data_context.h"
#include "../model/class_session.h"
#include "../utils/time_utils.h"

class ScheduleVerifier
{
public:
    // 1. Static Checks (Does the room fit? Is the type allowed?)
    static bool isRoomSuitable(const ClassSession &session, const Room &room, const std::string &buildingName);

    // 2. Dynamic Checks (Is everyone free?)
    static bool isSlotFree(const std::vector<ClassSession> &currentSchedule,
                           const ClassSession &candidate,
                           const std::string &day,
                           const std::string &start,
                           const std::string &end);
};