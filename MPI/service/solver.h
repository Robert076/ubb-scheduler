#pragma once
#include <vector>
#include <mpi.h>
#include "../repository/data_context.h"
#include "../model/class_session.h"
#include "schedule_verifier.h"

class Solver
{
private:
    const DataContext &ctx;

    // Helper: Try to place one session
    bool tryPlaceSession(ClassSession &session,
                         const std::vector<ClassSession> &scheduledSoFar);

    // Helper: Serialize session for MPI
    std::vector<char> serializeSession(const ClassSession &s);
    ClassSession deserializeSession(const std::vector<char> &data);

public:
    Solver(const DataContext &context) : ctx(context) {}

    // Original single-process solver (keep for compatibility)
    bool solve(std::vector<ClassSession> &sessions, int rank);

    // NEW: Collaborative solver where all processes work together
    bool solveCollaborative(std::vector<ClassSession> &sessions, int rank, int size);
};