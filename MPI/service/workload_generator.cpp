#include "workload_generator.h"
#include <iostream>

using namespace std;

vector<ClassSession> WorkloadGenerator::generate(const DataContext &ctx)
{
    vector<ClassSession> allSessions;

    // 1. Loop through every Group
    for (const auto &[groupId, group] : ctx.groups.getAll())
    {

        // 2. Loop through every Subject this group takes
        for (const auto &subjectName : group.getSubjects())
        {

            // Get the full subject details (config)
            const Subject &subject = ctx.subjects.getSubject(subjectName);

            // --- A. Generate COURSES ---
            // Courses are usually for the whole group (no split)
            int courseCount = (int)ceil(subject.getCoursesPerWeek());
            for (int i = 0; i < courseCount; i++)
            {
                ClassSession session;
                session.subjectName = subjectName;
                session.type = ClassType::COURSE;
                session.groupId = groupId;
                session.subGroup = "";                          // Whole group
                session.teacherName = subject.getMainTeacher(); // Default to main teacher

                // Duration matches the config
                // (We will use this later for time slotting)
                // session.duration = subject.getCourseLength();

                allSessions.push_back(session);
            }

            // --- B. Generate SEMINARS ---
            int semCount = (int)ceil(subject.getSeminarsPerWeek());
            if (semCount > 0)
            {
                // If group is split into 2 for seminars, we need sessions for Subgroup 1 AND 2
                int splits = group.getSeminarySplit();
                for (int s = 1; s <= splits; s++)
                {
                    for (int i = 0; i < semCount; i++)
                    {
                        ClassSession session;
                        session.subjectName = subjectName;
                        session.type = ClassType::SEMINARY;
                        session.groupId = groupId;

                        // If split is 1, it's the whole group. Otherwise, it's "1", "2", etc.
                        session.subGroup = (splits == 1) ? "" : to_string(s);

                        // Teacher is unknown for now (seminars might have different teachers)
                        // We can default to main teacher or leave empty
                        session.teacherName = subject.getMainTeacher();

                        allSessions.push_back(session);
                    }
                }
            }

            // --- C. Generate LABORATORIES ---
            int labCount = (int)ceil(subject.getLaboratoriesPerWeek());
            if (labCount > 0)
            {
                int splits = group.getLaboratorySplit();
                for (int s = 1; s <= splits; s++)
                {
                    for (int i = 0; i < labCount; i++)
                    {
                        ClassSession session;
                        session.subjectName = subjectName;
                        session.type = ClassType::LABORATORY;
                        session.groupId = groupId;
                        session.subGroup = (splits == 1) ? "" : to_string(s);

                        session.teacherName = subject.getMainTeacher();

                        allSessions.push_back(session);
                    }
                }
            }
        }
    }

    return allSessions;
}