#pragma once
#include <map>
#include <string>
#include <vector>
#include "../model/teacher.h"

class TeacherRepository
{
private:
    std::map<std::string, Teacher> teachers;

public:
    void load(const std::string &filename);

    const Teacher &getTeacher(const std::string &name) const;
    const std::map<std::string, Teacher> &getAll() const;
};