# MPI Collaborative Schedule Solver

## What This Does

University timetabling solver where **all MPI processes work together** to build **one schedule** instead of each finding their own solution independently.

## The Problem

- Schedule 231 class sessions (courses, seminaries, laboratories)
- Assign time slots, rooms, and teachers
- Avoid conflicts (same room, same teacher, same group at same time)
- Respect constraints (room capacity, building availability, teacher schedules)

## Architecture: Master-Worker Pattern

### Master Process (Rank 0)

**Job:** Coordinate everything and manage the global schedule state

**Data Structures:**

- `scheduledSessions` - sessions that have been successfully placed
- `unscheduled` - sessions still waiting to be scheduled
- `sessionsInProgress` - counter tracking how many workers are currently busy

**What It Does:**

1. Shuffles all unscheduled sessions (for randomness)
2. Waits for workers to either:
   - Request work (send them an unscheduled session + current schedule state)
   - Return results (add successful placements to `scheduledSessions`)
3. Tracks progress and terminates workers when done

### Worker Processes (Rank 1..N)

**Job:** Try to place individual sessions without conflicts

**What They Do:**

1. Request work from master
2. Receive: current schedule state + one unscheduled session
3. Try to find a valid slot (loop through buildings, days, hours, rooms)
4. Check if placement is valid using `ScheduleVerifier::isSlotFree()`
5. Send result back to master (success + placed session OR failure)
6. Repeat until master sends termination signal

## Key Functions

### `solveCollaborative(sessions, rank, size)`

Main entry point. Routes to master or worker logic based on rank.

### `tryPlaceSession(session, scheduledSoFar)`

Core placement algorithm:

- Try every building → day → hour → room combination
- Check static constraints (room type, capacity, building hours)
- Check dynamic constraints (room free? teacher free? group free?)
- Return true on first valid placement found

### `serializeSession()` / `deserializeSession()`

Pack/unpack ClassSession objects into byte arrays for MPI transmission.
Simple format: string lengths + string data + enum values + integers.

## MPI Communication Flow

```
MASTER                          WORKER
  |                               |
  |<------- TAG_WORK_REQUEST -----|  "Give me work"
  |                               |
  |---- TAG_WORK_ASSIGN --------->|  "Here's the current schedule + 1 session to place"
  |                               |
  |                               |  [Worker tries to place session]
  |                               |
  |<------ TAG_WORK_RESULT -------|  "Success! Here's the placed session"
  |                               |
  [Master adds to schedule]       |
  |                               |
  |<------- TAG_WORK_REQUEST -----|  [Loop continues...]
```

## Special Signals

- `numScheduled = -1`: Terminate (no more work)
- `numScheduled = -2`: Wait and retry (other workers still busy on last sessions)

## Why This Works

1. **Sequential Consistency**: Sessions are placed one at a time, so no conflicts
2. **Parallel Efficiency**: Multiple workers try placements simultaneously
3. **Shared State**: Every worker uses the same partial schedule
4. **Simple Synchronization**: Master is the single source of truth

## Run It

```bash
mpic++ -std=c++17 main.cpp service/*.cpp repository/*.cpp -o scheduler
mpirun -np 4 ./scheduler
```

Output: `schedule_output_0.json` with all 231 sessions placed in ~0.2 seconds.

## Bottlenecks to Know

- Master sends entire schedule state to each worker (grows over time)
- Last few sessions can cause idle workers (waiting for others to finish)
- No backtracking: if a session can't be placed, the whole solve fails

## Quick Debugging

If it hangs: check that workers send `TAG_WORK_RESULT` for every `TAG_WORK_ASSIGN` they receive.
