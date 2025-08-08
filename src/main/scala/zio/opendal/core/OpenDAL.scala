package zio.opendal.core

/**
 * Main OpenDAL trait that combines all operation interfaces
 *
 * This trait provides the complete API for interacting with OpenDAL storage
 * backends. It combines basic operations (read/write), file operations
 * (copy/rename), directory operations (list/createDir), presigned operations,
 * and info operations.
 */
trait OpenDAL
    extends BasicOperations
    with FileOperations
    with DirectoryOperations
    with PresignedOperations
    with InfoOperations
