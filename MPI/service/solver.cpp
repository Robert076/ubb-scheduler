#include "solver.h"
#include <algorithm>
#include <random>
#include <iostream>
#include <fstream>
#include <cstring>

using namespace std;

// MPI Tags
#define TAG_WORK_REQUEST 1
#define TAG_WORK_ASSIGN 2
#define TAG_WORK_RESULT 3
#define TAG_TERMINATE 4
#define TAG_FINAL_SCHEDULE 5

// Save function (unchanged)
void saveScheduleToFile(const vector<ClassSession> &sessions, int rank)
{
    string filename = "schedule_output_" + to_string(rank) + ".json";
    ofstream out(filename);

    out << "[\n";
    for (size_t i = 0; i < sessions.size(); ++i)
    {
        const auto &s = sessions[i];
        string typeStr = "Unknown";
        if (s.type == ClassType::COURSE)
            typeStr = "Course";
        else if (s.type == ClassType::SEMINARY)
            typeStr = "Seminar";
        else if (s.type == ClassType::LABORATORY)
            typeStr = "Laboratory";

        string freqStr = "Weekly";
        if (s.weekMask == 1)
            freqStr = "Odd Week";
        if (s.weekMask == 2)
            freqStr = "Even Week";

        out << "  {\n";
        out << "    \"day\": \"" << s.day << "\",\n";
        out << "    \"start\": \"" << s.startTime << "\",\n";
        out << "    \"end\": \"" << s.endTime << "\",\n";
        out << "    \"type\": \"" << typeStr << "\",\n";
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

// Helper: Try to place a single session
bool Solver::tryPlaceSession(ClassSession &session, const vector<ClassSession> &scheduledSoFar)
{
    for (const auto &[buildingName, place] : ctx.places.getAll())
    {
        vector<string> days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

        for (const auto &day : days)
        {
            for (int h = 8; h < 20; h++)
            {
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
                    duration = 2;

                string start = (h < 10 ? "0" : "") + to_string(h) + ":00";
                string end = (h + duration < 10 ? "0" : "") + to_string(h + duration) + ":00";

                if (h + duration > 20)
                    continue;

                for (const auto &[roomName, room] : place.getRooms())
                {
                    if (!ScheduleVerifier::isRoomSuitable(session, room, buildingName))
                        continue;

                    int groupSize = ctx.groups.getGroup(session.groupId).getSize();
                    if (!session.subGroup.empty())
                        groupSize /= 2;
                    if (room.capacity < groupSize)
                        continue;

                    session.roomName = roomName;
                    session.buildingName = buildingName;
                    session.day = day;
                    session.startTime = start;
                    session.endTime = end;

                    if (ScheduleVerifier::isSlotFree(scheduledSoFar, session, day, start, end))
                    {
                        return true;
                    }
                }
            }
        }
    }
    return false;
}

// Simple serialization: pack session into char vector
vector<char> Solver::serializeSession(const ClassSession &s)
{
    vector<char> buffer;

    auto addString = [&](const string &str)
    {
        int len = str.size();
        buffer.insert(buffer.end(), (char *)&len, (char *)&len + sizeof(int));
        buffer.insert(buffer.end(), str.begin(), str.end());
    };

    addString(s.subjectName);
    buffer.push_back((char)s.type);
    addString(s.groupId);
    addString(s.subGroup);
    addString(s.teacherName);
    addString(s.buildingName);
    addString(s.roomName);
    addString(s.day);
    addString(s.startTime);
    addString(s.endTime);
    buffer.insert(buffer.end(), (char *)&s.weekMask, (char *)&s.weekMask + sizeof(int));

    return buffer;
}

ClassSession Solver::deserializeSession(const vector<char> &data)
{
    ClassSession s;
    size_t pos = 0;

    auto readString = [&]() -> string
    {
        int len;
        memcpy(&len, &data[pos], sizeof(int));
        pos += sizeof(int);
        string str(data.begin() + pos, data.begin() + pos + len);
        pos += len;
        return str;
    };

    s.subjectName = readString();
    s.type = (ClassType)data[pos++];
    s.groupId = readString();
    s.subGroup = readString();
    s.teacherName = readString();
    s.buildingName = readString();
    s.roomName = readString();
    s.day = readString();
    s.startTime = readString();
    s.endTime = readString();
    memcpy(&s.weekMask, &data[pos], sizeof(int));

    return s;
}

// NEW: Collaborative solver using master-worker pattern
bool Solver::solveCollaborative(vector<ClassSession> &sessions, int rank, int size)
{
    if (rank == 0)
    {
        // MASTER PROCESS
        vector<ClassSession> scheduledSessions;
        vector<ClassSession> unscheduled = sessions;

        // Shuffle for variety
        mt19937 g(42);
        shuffle(unscheduled.begin(), unscheduled.end(), g);

        int nextSessionIdx = 0;
        int sessionsInProgress = 0; // Track how many workers are currently working
        int completedSessions = 0;

        cout << "[Master] Starting collaborative solve with " << (size - 1) << " workers" << endl;

        // Main loop: continue while there's work to do or workers are busy
        while (completedSessions < (int)sessions.size())
        {
            MPI_Status status;
            int dummy;

            // Wait for a worker to request work or send results
            // First check for results
            int flag;
            MPI_Iprobe(MPI_ANY_SOURCE, TAG_WORK_RESULT, MPI_COMM_WORLD, &flag, &status);

            if (flag)
            {
                // Process result
                int success;
                MPI_Recv(&success, 1, MPI_INT, status.MPI_SOURCE, TAG_WORK_RESULT, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
                sessionsInProgress--;

                cout << "[Master] DEBUG: Received result from worker " << status.MPI_SOURCE
                     << ", success=" << success << ", inProgress=" << sessionsInProgress << endl;

                if (success)
                {
                    // Receive the scheduled session
                    int bufSize;
                    MPI_Recv(&bufSize, 1, MPI_INT, status.MPI_SOURCE, TAG_WORK_RESULT, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
                    vector<char> buf(bufSize);
                    MPI_Recv(buf.data(), bufSize, MPI_CHAR, status.MPI_SOURCE, TAG_WORK_RESULT, MPI_COMM_WORLD, MPI_STATUS_IGNORE);

                    ClassSession scheduled = deserializeSession(buf);
                    scheduledSessions.push_back(scheduled);
                    completedSessions++;

                    if (completedSessions % 10 == 0 || completedSessions == (int)sessions.size())
                    {
                        cout << "[Master] Progress: " << completedSessions << "/" << sessions.size() << " sessions scheduled" << endl;
                    }
                }
                else
                {
                    cout << "[Master] ERROR: Worker failed to place session at index " << (nextSessionIdx - 1) << endl;
                    sessions.clear();

                    // Terminate all workers
                    for (int i = 1; i < size; i++)
                    {
                        int terminate = -1;
                        MPI_Send(&terminate, 1, MPI_INT, i, TAG_WORK_ASSIGN, MPI_COMM_WORLD);
                    }
                    return false;
                }
                continue; // Check for more results before handling work requests
            }

            // Now handle work requests
            MPI_Recv(&dummy, 1, MPI_INT, MPI_ANY_SOURCE, TAG_WORK_REQUEST, MPI_COMM_WORLD, &status);
            int workerRank = status.MPI_SOURCE;

            cout << "[Master] DEBUG: Worker " << workerRank << " requesting work. nextIdx="
                 << nextSessionIdx << "/" << unscheduled.size() << ", completed="
                 << completedSessions << "/" << sessions.size() << endl;

            if (nextSessionIdx < (int)unscheduled.size())
            {
                // Send current scheduled state and next unscheduled session
                int numScheduled = scheduledSessions.size();
                MPI_Send(&numScheduled, 1, MPI_INT, workerRank, TAG_WORK_ASSIGN, MPI_COMM_WORLD);

                // Send all scheduled sessions
                for (const auto &s : scheduledSessions)
                {
                    vector<char> buf = serializeSession(s);
                    int bufSize = buf.size();
                    MPI_Send(&bufSize, 1, MPI_INT, workerRank, TAG_WORK_ASSIGN, MPI_COMM_WORLD);
                    MPI_Send(buf.data(), bufSize, MPI_CHAR, workerRank, TAG_WORK_ASSIGN, MPI_COMM_WORLD);
                }

                // Send the unscheduled session to work on
                vector<char> buf = serializeSession(unscheduled[nextSessionIdx]);
                int bufSize = buf.size();
                MPI_Send(&bufSize, 1, MPI_INT, workerRank, TAG_WORK_ASSIGN, MPI_COMM_WORLD);
                MPI_Send(buf.data(), bufSize, MPI_CHAR, workerRank, TAG_WORK_ASSIGN, MPI_COMM_WORLD);

                nextSessionIdx++;
                sessionsInProgress++;

                cout << "[Master] DEBUG: Assigned work to worker " << workerRank
                     << ", inProgress=" << sessionsInProgress << endl;
            }
            else
            {
                // No more work to assign
                if (sessionsInProgress == 0)
                {
                    // All done - terminate worker
                    int terminate = -1;
                    MPI_Send(&terminate, 1, MPI_INT, workerRank, TAG_WORK_ASSIGN, MPI_COMM_WORLD);
                    cout << "[Master] DEBUG: Terminating worker " << workerRank << " (no work left)" << endl;
                }
                else
                {
                    // Workers still processing - tell this worker to wait and ask again
                    int wait = -2; // Special signal: wait and retry
                    MPI_Send(&wait, 1, MPI_INT, workerRank, TAG_WORK_ASSIGN, MPI_COMM_WORLD);
                    cout << "[Master] DEBUG: Told worker " << workerRank << " to wait (inProgress="
                         << sessionsInProgress << ")" << endl;
                }
            }
        }

        cout << "[Master] DEBUG: Main loop done. Terminating remaining workers." << endl;

        // Terminate all workers
        for (int i = 1; i < size; i++)
        {
            int terminate = -1;
            MPI_Send(&terminate, 1, MPI_INT, i, TAG_WORK_ASSIGN, MPI_COMM_WORLD);
        }

        sessions = scheduledSessions;
        return completedSessions == (int)sessions.size();
    }
    else
    {
        // WORKER PROCESS
        while (true)
        {
            // Request work from master
            int dummy = 0;
            MPI_Send(&dummy, 1, MPI_INT, 0, TAG_WORK_REQUEST, MPI_COMM_WORLD);

            // Receive number of scheduled sessions (or termination signal)
            int numScheduled;
            MPI_Recv(&numScheduled, 1, MPI_INT, 0, TAG_WORK_ASSIGN, MPI_COMM_WORLD, MPI_STATUS_IGNORE);

            cout << "[Worker " << rank << "] DEBUG: Received numScheduled=" << numScheduled << endl;

            if (numScheduled == -1)
            {
                // Termination signal
                cout << "[Worker " << rank << "] DEBUG: Received termination signal" << endl;
                break;
            }

            if (numScheduled == -2)
            {
                // Wait signal - other workers still busy, retry
                cout << "[Worker " << rank << "] DEBUG: Told to wait, retrying..." << endl;
                continue;
            }

            // Receive all scheduled sessions
            vector<ClassSession> scheduledSoFar;
            for (int i = 0; i < numScheduled; i++)
            {
                int bufSize;
                MPI_Recv(&bufSize, 1, MPI_INT, 0, TAG_WORK_ASSIGN, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
                vector<char> buf(bufSize);
                MPI_Recv(buf.data(), bufSize, MPI_CHAR, 0, TAG_WORK_ASSIGN, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
                scheduledSoFar.push_back(deserializeSession(buf));
            }

            // Receive the unscheduled session to work on
            int bufSize;
            MPI_Recv(&bufSize, 1, MPI_INT, 0, TAG_WORK_ASSIGN, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
            vector<char> buf(bufSize);
            MPI_Recv(buf.data(), bufSize, MPI_CHAR, 0, TAG_WORK_ASSIGN, MPI_COMM_WORLD, MPI_STATUS_IGNORE);
            ClassSession toSchedule = deserializeSession(buf);

            cout << "[Worker " << rank << "] DEBUG: Attempting to place session: "
                 << toSchedule.subjectName << " for group " << toSchedule.groupId << endl;

            // Try to place the session
            bool success = tryPlaceSession(toSchedule, scheduledSoFar);

            cout << "[Worker " << rank << "] DEBUG: Placement result: " << (success ? "SUCCESS" : "FAILED") << endl;

            // Send result back
            int successFlag = success ? 1 : 0;
            MPI_Send(&successFlag, 1, MPI_INT, 0, TAG_WORK_RESULT, MPI_COMM_WORLD);

            if (success)
            {
                vector<char> resultBuf = serializeSession(toSchedule);
                int resultSize = resultBuf.size();
                MPI_Send(&resultSize, 1, MPI_INT, 0, TAG_WORK_RESULT, MPI_COMM_WORLD);
                MPI_Send(resultBuf.data(), resultSize, MPI_CHAR, 0, TAG_WORK_RESULT, MPI_COMM_WORLD);
            }
        }

        sessions.clear();
        return true;
    }
}

// Original solve function (kept for compatibility)
bool Solver::solve(vector<ClassSession> &sessions, int rank)
{
    std::mt19937 g(rank + 1);
    std::shuffle(sessions.begin(), sessions.end(), g);

    vector<ClassSession> scheduledSoFar;

    for (auto &session : sessions)
    {
        if (!tryPlaceSession(session, scheduledSoFar))
            return false;
        scheduledSoFar.push_back(session);
    }

    sessions = scheduledSoFar;
    return true;
}