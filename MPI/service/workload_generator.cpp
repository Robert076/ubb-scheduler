#include "workload_generator.h"
#include <iostream>
#include <cmath>

using namespace std;

vector<ClassSession> WorkloadGenerator::generate(const DataContext &ctx)
{
    vector<ClassSession> allSessions;

    for (const auto &[groupId, group] : ctx.groups.getAll())
    {

        for (const auto &subjectName : group.getSubjects())
        {
            if (ctx.subjects.getAll().find(subjectName) == ctx.subjects.getAll().end())
            {
                continue;
            }
            const Subject &subject = ctx.subjects.getSubject(subjectName);

            int courseCount = (int)ceil(subject.getCoursesPerWeek());
            for (int i = 0; i < courseCount; i++)
            {
                ClassSession session;
                session.subjectName = subjectName;
                session.type = ClassType::COURSE;
                session.groupId = groupId;
                session.subGroup = ""; // whole group
                session.teacherName = subject.getMainTeacher();
                session.weekMask = 3;

                allSessions.push_back(session);
            }

            int semCount = (int)ceil(subject.getSeminarsPerWeek());
            if (semCount > 0)
            {
                int splits = group.getSeminarySplit();

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

                        if (isBiWeekly)
                        {
                            if (splits >= 2)
                                session.weekMask = (s % 2 != 0) ? 1 : 2;
                            else
                                session.weekMask = 1;
                        }
                        else
                        {
                            session.weekMask = 3;
                        }

                        allSessions.push_back(session);
                    }
                }
            }

            int labCount = (int)ceil(subject.getLaboratoriesPerWeek());
            if (labCount > 0)
            {
                int splits = group.getLaboratorySplit();
                if (subject.getLabSplitOverride() > 0)
                {
                    splits = subject.getLabSplitOverride();
                }

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

                        if (isBiWeekly)
                        {
                            if (splits >= 2)
                                session.weekMask = (s % 2 != 0) ? 1 : 2;
                            else
                                session.weekMask = 1;
                        }
                        else
                        {
                            session.weekMask = 3;
                        }

                        allSessions.push_back(session);
                    }
                }
            }
        }
    }

    return allSessions;
}