#include "solver.h"
#include <algorithm>
#include <random>
#include <fstream>
#include <iostream>

using namespace std;

bool Solver::solve(vector<ClassSession> &sessions, int rank)
{
    // 1. Shuffle the sessions based on Rank
    // This ensures every MPI node tries a different order!
    std::mt19937 g(rank + 1); // Seed with rank
    std::shuffle(sessions.begin(), sessions.end(), g);

    // We need to keep track of successfully scheduled sessions to check for conflicts
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

                    // --- FIX START: Calculate Duration FIRST ---
                    int duration = 2; // Default fallback
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
                    // --- FIX END ---

                    // Now we can calculate the end time safely
                    string start = (h < 10 ? "0" : "") + to_string(h) + ":00";
                    string end = (h + duration < 10 ? "0" : "") + to_string(h + duration) + ":00";

                    // Stop if this session goes past 8 PM (20:00)
                    if (h + duration > 20)
                        continue;

                    // Try every Room
                    for (const auto &[roomName, room] : place.getRooms())
                    {

                        // 1. Static Check (Capacity, Flags)
                        if (!ScheduleVerifier::isRoomSuitable(session, room, buildingName))
                            continue;

                        // 2. Check Capacity
                        int groupSize = ctx.groups.getGroup(session.groupId).getSize();
                        // If it's a split (lab/seminar), half the size roughly
                        if (!session.subGroup.empty())
                            groupSize /= 2;

                        if (room.capacity < groupSize)
                            continue;

                        // 3. Dynamic Check (Conflicts)
                        session.roomName = roomName; // Tentative assignment
                        session.buildingName = buildingName;
                        session.day = day;
                        session.startTime = start;
                        session.endTime = end;

                        if (ScheduleVerifier::isSlotFree(scheduledSoFar, session, day, start, end))
                        {
                            // FOUND A SLOT!
                            placed = true;
                            scheduledSoFar.push_back(session);
                            break;
                        }
                    }
                }
            }
        }

        if (!placed)
        {
            // Greedy failure: Could not place this specific session anywhere
            return false;
        }
    }

    // Success! Copy the result back
    sessions = scheduledSoFar;
    return true;
}

void saveScheduleToFile(const vector<ClassSession> &sessions, int rank)
{
    // Create a filename like "schedule_rank_0.json"
    string filename = "schedule_output_" + to_string(rank) + ".json";
    ofstream out(filename);

    // Manual JSON construction to avoid dependency issues, or use nlohmann/json
    out << "[\n";
    for (size_t i = 0; i < sessions.size(); ++i)
    {
        const auto &s = sessions[i];
        out << "  {\n";
        out << "    \"day\": \"" << s.day << "\",\n";
        out << "    \"start\": \"" << s.startTime << "\",\n";
        out << "    \"end\": \"" << s.endTime << "\",\n";
        out << "    \"group\": \"" << s.groupId << "\",\n";
        out << "    \"subgroup\": \"" << s.subGroup << "\",\n";
        out << "    \"subject\": \"" << s.subjectName << "\",\n";
        out << "    \"room\": \"" << s.roomName << "\"\n";
        out << "  }" << (i < sessions.size() - 1 ? "," : "") << "\n";
    }
    out << "]\n";
    out.close();
    cout << "[Rank " << rank << "] Saved schedule to " << filename << endl;
}