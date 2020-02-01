package edu.coursera.distributed;

import edu.coursera.distributed.util.MPI;
import edu.coursera.distributed.util.MPI.MPIException;
import edu.coursera.distributed.util.MPI.MPI_Request;

/**
 * A wrapper class for a parallel, MPI-based matrix multiply implementation.
 */
public class MatrixMult {
    /**
     * A parallel implementation of matrix multiply using MPI to express SPMD
     * parallelism. In particular, this method should store the output of
     * multiplying the matrices a and b into the matrix c.
     * <p>
     * This method is called simultaneously by all MPI ranks in a running MPI
     * program. For simplicity MPI_Init has already been called, and
     * MPI_Finalize should not be called in parallelMatrixMultiply.
     * <p>
     * On entry to parallelMatrixMultiply, the following will be true of a, b,
     * and c:
     * <p>
     * 1) The matrix a will only be filled with the input values on MPI rank
     * zero. Matrix a on all other ranks will be empty (initialized to all
     * zeros).
     * 2) Likewise, the matrix b will only be filled with input values on MPI
     * rank zero. Matrix b on all other ranks will be empty (initialized to
     * all zeros).
     * 3) Matrix c will be initialized to all zeros on all ranks.
     * <p>
     * Upon returning from parallelMatrixMultiply, the following must be true:
     * <p>
     * 1) On rank zero, matrix c must be filled with the final output of the
     * full matrix multiplication. The contents of matrix c on all other
     * ranks are ignored.
     * <p>
     * Therefore, it is the responsibility of this method to distribute the
     * input data in a and b across all MPI ranks for maximal parallelism,
     * perform the matrix multiply in parallel, and finally collect the output
     * data in c from all ranks back to the zeroth rank. You may use any of the
     * MPI APIs provided in the mpi object to accomplish this.
     * <p>
     * A reference sequential implementation is provided below, demonstrating
     * the use of the Matrix class's APIs.
     *
     * @param a   Input matrix
     * @param b   Input matrix
     * @param c   Output matrix
     * @param mpi MPI object supporting MPI APIs
     * @throws MPIException On MPI error. It is not expected that your
     *                      implementation should throw any MPI errors during
     *                      normal operation.
     */
    public static void parallelMatrixMultiply(final Matrix a, final Matrix b, final Matrix c, final MPI mpi)
            throws MPIException {
        // distribute matrices A and B to all other nodes
        mpi.MPI_Bcast(a.getValues(), 0, a.getValues().length, 0, mpi.MPI_COMM_WORLD);
        mpi.MPI_Bcast(b.getValues(), 0, b.getValues().length, 0, mpi.MPI_COMM_WORLD);

        final int rank = mpi.MPI_Comm_rank(mpi.MPI_COMM_WORLD);
        final int size = mpi.MPI_Comm_size(mpi.MPI_COMM_WORLD);

        final int rows = c.getNRows();
        final int cols = c.getNCols();

        final int chunk = (rows + size - 1) / size;
        final int startRow = getStartRow(rank, chunk);
        final int endRow = getEndRow(rank, chunk, rows);

        for (int i = 0; i < startRow; i++) {
            for (int j = 0; j < endRow; j++) {
                c.set(i, j, 0.0);

                for (int k = 0; k < b.getNRows(); k++) {
                    c.incr(i, j, a.get(i, k) * b.get(k, j));
                }
            }
        }

        if (rank == 0) {
            MPI_Request[] requests = new MPI_Request[size - 1];

            for (int i = 1; i < size; i++) {
                final int nodeStartRow = getStartRow(i, chunk);
                final int nodeEndRow = getEndRow(i, chunk, rows);
                final int offset = getOffset(nodeStartRow, cols);
                final int count = getCount(nodeStartRow, nodeEndRow, cols);

                requests[i - 1] = mpi.MPI_Irecv(c.getValues(), offset, count, i, i, mpi.MPI_COMM_WORLD);
            }
            mpi.MPI_Waitall(requests);
        } else {
            final int offset = getOffset(startRow, cols);
            final int count = getCount(startRow, endRow, cols);

            mpi.MPI_Isend(c.getValues(), offset, count, 0, rank, mpi.MPI_COMM_WORLD);
        }
    }

    private static int getStartRow(final int rank, final int chunk) {
        return rank * chunk;
    }

    private static int getEndRow(final int rank, final int chunk, final int rows) {
        return Math.min((rank + 1) * chunk, rows);
    }

    private static int getOffset(final int startRow, final int cols) {
        return startRow * cols;
    }

    private static int getCount(final int startRow, final int endRow, final int cols) {
        return (endRow - startRow) * cols;
    }
}
