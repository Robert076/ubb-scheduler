#include <mpi.h>
#include "repository/data_context.h"
#include "service/workload_generator.h"
#include "service/solver.h"
#include <iostream>

// Forward declaration of the helper we just added (or put it in a header)
void saveScheduleToFile(const std::vector<ClassSession> &sessions, int rank);

int main(int argc, char **argv)
{
    MPI_Init(&argc, &argv);

    int rank, size;
    MPI_Comm_rank(MPI_COMM_WORLD, &rank);
    MPI_Comm_size(MPI_COMM_WORLD, &size);

    DataContext ctx;
    ctx.loadAll("config");

    WorkloadGenerator generator;
    std::vector<ClassSession> sessions = generator.generate(ctx);

    if (rank == 0)
    {
        std::cout << "--- MPI Schedule Solver Started ---" << std::endl;
    }

    Solver solver(ctx);
    double startTime = MPI_Wtime();

    // Run the solver
    bool success = solver.solve(sessions, rank);

    double endTime = MPI_Wtime();

    if (success)
    {
        std::cout << "[Rank " << rank << "] FOUND A SOLUTION in "
                  << (endTime - startTime) << "s!" << std::endl;

        // Save to file instead of crashing
        saveScheduleToFile(sessions, rank);
    }
    else
    {
        std::cout << "[Rank " << rank << "] Could not find a schedule." << std::endl;
    }

    // Wait for everyone to finish before quitting (Clean Exit)
    MPI_Barrier(MPI_COMM_WORLD);
    MPI_Finalize();
    return 0;
}