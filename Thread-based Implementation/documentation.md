# University Schedule Manager v1.0 - Technical Guide

## Architecture & File Dependencies

**Initialization Phase:**
- `Main.java` → `TimetableInitializer.java` → 4 Repositories (Group, Teacher, Subject, Place)
- Repositories load config files and convert to Java objects
- `TimetableDataContext.java` builds immutable singleton with pre-computed indices (teacher→subjects, subject→groups, building→teachers, room→capabilities)

**Validation Phase:**
- `ValidationOrchestrator.java` runs 5 validators in order:
    - V0: `TeacherDefinitionValidator` (sequential, blocking)
    - V1-V4: `TeacherCapacityValidator`, `RoomCapacityValidator`, `TeacherAvailabilityValidator`, `TimeSlotCollisionValidator` (parallel)
- Results aggregated and displayed via `ConsoleUI.java`

**Generation Phase:**
- `TimetableGenerator.java` creates thread pool (CPU cores count)
- Subjects sorted by hours (hardest first) and distributed to threads
- Each subject processed by `SubjectScheduler.java` using backtracking algorithm
- Shared state objects: `TeacherScheduleState`, `RoomScheduleState`, `GroupScheduleState` (thread-safe)
- Results collected in `GenerationResult` and exported via `ScheduleExporter.java`

---

## Multithreading Operations

| Component | Threading | Complexity | Details |
|-----------|-----------|------------|---------|
| **TeacherCapacityValidator (V1+V5)** | Parallel by subjects | O(subjects × teachers) | Each subject analyzed on separate thread; checks teacher hours vs. required hours + language matching |
| **RoomCapacityValidator (V2)** | Parallel by places | O(places × rooms) | Each building evaluated independently; validates room capacity per activity type |
| **TeacherAvailabilityValidator (V3+V6)** | Sequential aggregate | O(teachers + buildings) | Global stats: total teacher hours vs. total required; building distribution analysis |
| **TimeSlotCollisionValidator (V4)** | Parallel matrix build | O(slots × days) | Builds 2D collision matrix; detects teacher/room bottlenecks at specific times |
| **TimetableGenerator (Generation)** | Parallel by subjects | O(subjects) with backtracking | Fixed thread pool = CPU cores; each subject scheduled independently; uses ConcurrentHashMap for state |

**Blocking Rules:**
- V0 fails → stops entire validation pipeline
- V1-V4 use `ExecutorService.submit()` + `Future.get()` for synchronization
- WARN status never blocks; only FAIL blocks generation

---

## Metrics Interpretation

`GenerationMetrics.printSummary()` outputs at completion:

### Total Time
Time elapsed from generation start to finish (ms)
- < 1000ms: Small schedules
- 1000–5000ms: Medium schedules, efficient parallelism
- > 5000ms: Large schedules or strict constraints; consider optimization

### Phase Timings
Breakdown of INITIALIZATION, VALIDATION, GENERATION phases
- High GENERATION time = backtracking bottleneck
- High VALIDATION time = index optimization needed

### Subject Metrics
Per-subject data:
- `scheduledHours`: Actual hours placed (compare vs. required)
- `executionTime`: Duration of scheduling that subject
- `success`: Boolean; false if < 100% hours allocated

### Success Rate (%)
`(Total scheduled hours) / (Total required hours) × 100%`

| Rate | Status | Action |
|------|--------|--------|
| 100% | ✓ Complete | Export and implement |
| 95–99% | ⚠ Near-complete | Review failed subjects; minor adjustments needed |
| 85–94% | ✗ Partial | 6–15% deficit; relax teacher preferences or add resources |
| < 85% | ✗ Incomplete | System overloaded; do not export |

### Errors
Diagnostic messages for failed subjects or exceptions:
- `"Subject X failed: Only Y/Z hours scheduled"` → Incomplete placement
- `"Execution error for subject X: [exception]"` → Scheduler crash for that subject

---