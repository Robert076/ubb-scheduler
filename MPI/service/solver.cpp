#include "solver.h"
#include <algorithm>
#include <random>
#include <iostream>
#include <fstream>

using namespace std;

// Helper to save output with TYPE included
void saveScheduleToFile(const vector<ClassSession> &sessions, int rank)
{
    string filename = "schedule_output_" + to_string(rank) + ".json";
    ofstream out(filename);

    out << "[\n";
    for (size_t i = 0; i < sessions.size(); ++i)
    {
        const auto &s = sessions[i];

        // 1. Determine Type String
        string typeStr = "Unknown";
        if (s.type == ClassType::COURSE)
            typeStr = "Course";
        else if (s.type == ClassType::SEMINARY)
            typeStr = "Seminar";
        else if (s.type == ClassType::LABORATORY)
            typeStr = "Laboratory";

        // 2. Determine Frequency String
        string freqStr = "Weekly";
        if (s.weekMask == 1)
            freqStr = "Odd Week";
        if (s.weekMask == 2)
            freqStr = "Even Week";

        out << "  {\n";
        out << "    \"day\": \"" << s.day << "\",\n";
        out << "    \"start\": \"" << s.startTime << "\",\n";
        out << "    \"end\": \"" << s.endTime << "\",\n";
        out << "    \"type\": \"" << typeStr << "\",\n"; // <--- ADDED HERE
        out << "    \"group\": \"" << s.groupId << "\",\n";
        out << "    \"subgroup\": \"" << s.subGroup << "\",\n";
        out << "    \"subject\": \"" << s.subjectName << "\",\n";
        out << "    \"teacher\": \"" << s.teacherName << "\",\n";
        out << "    \"room\": \"" << s.roomName << "\",\n";
        out << "    \"frequency\": \"" << freqStr << "\"\n";
        out << "  }" << (i < sessions.size() - 1 ? "," : "") << "\n";
    }
    out << "]\n";
    out.close();
    cout << "[Rank " << rank << "] Saved schedule to " << filename << endl;
}

bool Solver::solve(vector<ClassSession> &sessions, int rank)
{
    // 1. Shuffle
    std::mt19937 g(rank + 1);
    std::shuffle(sessions.begin(), sessions.end(), g);

    vector<ClassSession> scheduledSoFar;

    // 2. Iterate through every unscheduled session
    for (auto &session : sessions)
    {
        bool placed = false;

        // --- THE SEARCH LOOP ---
        for (const auto &[buildingName, place] : ctx.places.getAll())
        {
            if (placed)
                break;

            vector<string> days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

            for (const auto &day : days)
            {
                if (placed)
                    break;

                // Try hours from 08:00 to 20:00
                for (int h = 8; h < 20; h++)
                {
                    if (placed)
                        break;

                    // Calculate Duration
                    int duration = 2;
                    if (ctx.subjects.getAll().count(session.subjectName))
                    {
                        const auto &sub = ctx.subjects.getSubject(session.subjectName);
                        if (session.type == ClassType::COURSE)
                            duration = sub.getCourseLength();
                        else if (session.type == ClassType::SEMINARY)
                            duration = sub.getSeminarLength();
                        else
                            duration = sub.getLaboratoryLength();
                    }

                    if (duration == 0)
                        duration = 2; // Safety fallback

                    string start = (h < 10 ? "0" : "") + to_string(h) + ":00";
                    string end = (h + duration < 10 ? "0" : "") + to_string(h + duration) + ":00";

                    if (h + duration > 20)
                        continue;

                    for (const auto &[roomName, room] : place.getRooms())
                    {

                        // 1. Static Checks
                        if (!ScheduleVerifier::isRoomSuitable(session, room, buildingName))
                            continue;

                        int groupSize = ctx.groups.getGroup(session.groupId).getSize();
                        if (!session.subGroup.empty())
                            groupSize /= 2;
                        if (room.capacity < groupSize)
                            continue;

                        // 2. Dynamic Check
                        session.roomName = roomName;
                        session.buildingName = buildingName;
                        session.day = day;
                        session.startTime = start;
                        session.endTime = end;

                        if (ScheduleVerifier::isSlotFree(scheduledSoFar, session, day, start, end))
                        {
                            placed = true;
                            scheduledSoFar.push_back(session);
                            break;
                        }
                    }
                }
            }
        }

        if (!placed)
            return false;
    }

    sessions = scheduledSoFar;
    return true;
}