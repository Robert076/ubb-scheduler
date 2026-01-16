#pragma once
#include <vector>
#include <cmath> // for ceil
#include "../repository/data_context.h"
#include "../model/class_session.h"

class WorkloadGenerator
{
public:
    std::vector<ClassSession> generate(const DataContext &ctx);
};