#include "workload_generator.h"
#include <iostream>
#include <cmath>

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
            if (ctx.subjects.getAll().find(subjectName) == ctx.subjects.getAll().end())
            {
                continue; // Skip if subject not found in config
            }
            const Subject &subject = ctx.subjects.getSubject(subjectName);

            // --- A. Generate COURSES ---
            int courseCount = (int)ceil(subject.getCoursesPerWeek());
            for (int i = 0; i < courseCount; i++)
            {
                ClassSession session;
                session.subjectName = subjectName;
                session.type = ClassType::COURSE;
                session.groupId = groupId;
                session.subGroup = ""; // Whole group
                session.teacherName = subject.getMainTeacher();
                session.weekMask = 3; // Weekly

                allSessions.push_back(session);
            }

            // --- B. Generate SEMINARS ---
            int semCount = (int)ceil(subject.getSeminarsPerWeek());
            if (semCount > 0)
            {
                int splits = group.getSeminarySplit();

                // Check if bi-weekly (0.5 frequency)
                bool isBiWeekly = (subject.getSeminarsPerWeek() <= 0.501 && subject.getSeminarsPerWeek() > 0);

                for (int s = 1; s <= splits; s++)
                {
                    for (int i = 0; i < semCount; i++)
                    {
                        ClassSession session;
                        session.subjectName = subjectName;
                        session.type = ClassType::SEMINARY;
                        session.groupId = groupId;
                        session.subGroup = (splits == 1) ? "" : to_string(s);
                        session.teacherName = subject.getMainTeacher();

                        // Mask Logic
                        if (isBiWeekly)
                        {
                            if (splits >= 2)
                                session.weekMask = (s % 2 != 0) ? 1 : 2; // Alternate 1, 2
                            else
                                session.weekMask = 1; // Default to Odd
                        }
                        else
                        {
                            session.weekMask = 3; // Weekly
                        }

                        allSessions.push_back(session);
                    }
                }
            }

            // --- C. Generate LABORATORIES ---
            int labCount = (int)ceil(subject.getLaboratoriesPerWeek());
            if (labCount > 0)
            {
                // 1. Determine Split (Group Default vs Subject Override)
                int splits = group.getLaboratorySplit();
                if (subject.getLabSplitOverride() > 0)
                {
                    splits = subject.getLabSplitOverride();
                }

                // 2. Check if Bi-Weekly
                bool isBiWeekly = (subject.getLaboratoriesPerWeek() <= 0.501 && subject.getLaboratoriesPerWeek() > 0);

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

                        // Mask Logic
                        if (isBiWeekly)
                        {
                            // If we have multiple subgroups, alternate them to pack the schedule
                            // Subgroup 1 -> Odd, Subgroup 2 -> Even
                            if (splits >= 2)
                                session.weekMask = (s % 2 != 0) ? 1 : 2;
                            else
                                session.weekMask = 1; // Default to Odd
                        }
                        else
                        {
                            session.weekMask = 3; // Weekly
                        }

                        allSessions.push_back(session);
                    }
                }
            }
        }
    }

    return allSessions;
}