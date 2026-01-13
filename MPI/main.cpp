#include "repository/subject_repository.h"
#include "repository/teacher_repository.h" // <--- Include the new repo
#include <iostream>

int main()
{
    // 1. Test Subjects
    SubjectRepository subRepo;
    subRepo.load("config/subjects-config.json");

    // 2. Test Teachers
    TeacherRepository teacherRepo;
    teacherRepo.load("config/teachers-config.json");

    // Quick check
    try
    {
        const auto &t = teacherRepo.getTeacher("Gabriel Mircea");
        std::cout << "Test Passed: Found Teacher " << t.getName()
                  << " (Max hours: " << t.getMaxHours() << ")" << std::endl;
    }
    catch (...)
    {
        std::cout << "Teacher Test Failed" << std::endl;
    }

    return 0;
}