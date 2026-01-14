# Parallel University Scheduler (MPI)

C++ application for generating university schedules using MPI (Message Passing Interface). Resolves constraints (teachers, rooms, groups) using parallel independent search across multiple nodes.

## Build and Run

**Prerequisites:** Open MPI, `g++` (C++17), `nlohmann/json`.

```bash
# 1. Compile
mpic++ -std=c++17 main.cpp \
    repository/subject_repository.cpp \
    repository/teacher_repository.cpp \
    repository/place_repository.cpp \
    repository/group_repository.cpp \
    service/workload_generator.cpp \
    service/schedule_verifier.cpp \
    service/solver.cpp \
    -I include -o schedule_app_mpi

# 2. Run (example with 4 nodes)
mpirun -np 4 ./schedule_app_mpi
```

## Architecture

The system uses a layered architecture to model the scheduling problem and a randomized greedy strategy to solve it.

### 1. Data Layer (Repositories)

Loads configuration from JSON files into memory structures on initialization.

- **Subjects:** Course frequencies, durations, and types (Course, Seminar, Lab).
- **Teachers:** Availability schedules, building preferences, and subject qualifications.
- **Places:** Building and room hierarchy with specific constraints (e.g., `noLaboratory`).
- **Groups:** Student count, subgroup splits, and curriculum requirements.

### 2. Logic Layer

- **Workload Generator:** Converts configuration rules into a list of required `ClassSession` objects (e.g., handles subgroup splitting for labs).
- **Schedule Verifier:** Validates candidate slots against:
  - **Static Constraints:** Room capacity, room equipment/flags.
  - **Dynamic Constraints:** Teacher availability, group conflicts, time overlaps.

### 3. Solver (MPI Strategy)

Uses a **Parallel Independent Search** approach:

1.  **Initialization:** All nodes load identical configuration and generate the same unscheduled session list.
2.  **Randomization:** Each node shuffles the session list using its MPI rank as the seed.
3.  **Execution:** Nodes independently run a greedy algorithm to place sessions into valid time/room slots.
4.  **Termination:** Nodes output valid schedules to JSON files (e.g., `schedule_output_0.json`).

## Project structure

```
├── config/              # JSON configuration files
├── include/             # Dependencies (nlohmann/json.hpp)
├── model/               # Data definitions (Group, Teacher, Place, Session)
├── repository/          # JSON parsing and object mapping
├── service/             # Business logic
│   ├── workload_generator.cpp  # Session instantiation
│   ├── schedule_verifier.cpp   # Constraint checking
│   └── solver.cpp              # Search algorithm
└── main.cpp             # MPI initialization and process control
```

## Tech Stack

- **Language:** C++17
- **Parallelism:** Open MPI
- **Data Format:** JSON
- **Algorithm:** Randomized Greedy Constraint Satisfaction

## Output format

Schedules are saved as **JSON** format:

```json
[
  {
    "day": "Monday",
    "start": "08:00",
    "end": "10:00",
    "group": "911",
    "subgroup": "1",
    "subject": "Fundamentals of Programming",
    "room": "L404"
  }
]
```
