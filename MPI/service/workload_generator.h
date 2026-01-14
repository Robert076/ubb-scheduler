#pragma once
#include <vector>
#include <cmath> // for ceil
#include "../repository/data_context.h"
#include "../model/class_session.h"

class WorkloadGenerator
{
public:
    // Generates the list of ALL unassigned sessions needed for the university
    std::vector<ClassSession> generate(const DataContext &ctx);
};