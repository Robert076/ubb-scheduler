#include <mpi.h>
#include "repository/data_context.h"
#include "service/workload_generator.h"
#include "service/solver.h"
#include <iostream>
#include <vector>

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
        std::cout << "--- MPI Collaborative Schedule Solver Started ---" << std::endl;
        std::cout << "Total sessions to schedule: " << sessions.size() << std::endl;
        std::cout << "Using " << size << " processes" << std::endl;
    }

    Solver solver(ctx);
    double startTime = MPI_Wtime();

    bool success = solver.solveCollaborative(sessions, rank, size);

    double endTime = MPI_Wtime();

    if (rank == 0)
    {
        if (success)
        {
            std::cout << "\n[SUCCESS] Found complete solution in "
                      << (endTime - startTime) << " seconds!" << std::endl;
            saveScheduleToFile(sessions, rank);
        }
        else
        {
            std::cout << "\n[FAILED] Could not find a complete schedule." << std::endl;
        }
    }

    MPI_Barrier(MPI_COMM_WORLD);
    MPI_Finalize();
    return 0;
}