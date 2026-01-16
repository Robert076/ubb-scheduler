#include <mpi.h>
#include "repository/data_context.h"
#include "service/workload_generator.h"
#include "service/solver.h"
#include <iostream>

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

    bool success = solver.solve(sessions, rank);

    double endTime = MPI_Wtime();

    if (success)
    {
        std::cout << "[Rank " << rank << "] Found solution in "
                  << (endTime - startTime) << "seconds!" << std::endl;

        saveScheduleToFile(sessions, rank);
    }
    else
    {
        std::cout << "[Rank " << rank << "] Could not find a schedule." << std::endl;
    }

    MPI_Barrier(MPI_COMM_WORLD);
    MPI_Finalize();
    return 0;
}