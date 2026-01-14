#pragma once
#include <vector>
#include "../repository/data_context.h"
#include "../model/class_session.h"
#include "schedule_verifier.h"

class Solver
{
private:
    const DataContext &ctx;

public:
    Solver(const DataContext &context) : ctx(context) {}

    // Returns true if a valid schedule was found
    // The 'sessions' vector is modified in-place with the assigned times/rooms
    bool solve(std::vector<ClassSession> &sessions, int rank);
};