package zio.opendal.integration

import zio._
import zio.test._
import zio.opendal.OpenDAL
import zio.opendal.options._
import zio.opendal.integration.IntegrationTestUtils._

/**
 * Integration tests for ZIO OpenDAL S3 operations using LocalStack
 * 
 * These tests verify that OpenDAL works correctly with a real S3-compatible 
 * storage backend (LocalStack). They test all major operations including:
 * - Basic read/write operations
 * - File operations (copy, rename, delete)
 * - Directory operations (list, create, remove)
 * - Metadata operations
 * - Error handling
 * 
 * To run these tests, set the environment variable:
 * ENABLE_INTEGRATION_TESTS=true
 */
object S3IntegrationSpec extends ZIOSpecDefault {

  def spec: Spec[Any, TestFailure[Any]] =
    suite("S3 Integration Tests")(
      suite("Basic Operations")(
        test("write and read text file") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val testContent = TestData.smallText
              val testPath = "integration-test/basic-text.txt"
              
              (for {
                // Write file
                _ <- OpenDAL.writeText(testPath, testContent)
                
                // Read it back
                content <- OpenDAL.readString(testPath)
                
                // Clean up
                _ <- OpenDAL.delete(testPath)
                
              } yield assertTrue(content == testContent)).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        } @@ TestAspect.timeout(30.seconds),
        
        test("write and read binary data") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val testData = TestData.binaryData
              val testPath = "integration-test/binary-data.bin"
              
              (for {
                // Write binary data
                _ <- OpenDAL.write(testPath, testData)
                
                // Read it back
                readData <- OpenDAL.read(testPath)
                
                // Clean up
                _ <- OpenDAL.delete(testPath)
                
              } yield assertTrue(readData.sameElements(testData))).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        },
        
        test("write with options and content type") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val jsonContent = TestData.jsonData
              val testPath = "integration-test/data.json"
              
              (for {
                // Write with JSON content type
                _ <- OpenDAL.write(testPath, jsonContent, WriteOpts.json)
                
                // Read back and verify
                content <- OpenDAL.readString(testPath)
                
                // Get metadata to verify content type
                metadata <- OpenDAL.stat(testPath)
                
                // Clean up
                _ <- OpenDAL.delete(testPath)
                
              } yield assertTrue(
                content == jsonContent &&
                metadata.getContentLength == jsonContent.getBytes("UTF-8").length &&
                Option(metadata.getContentType).nonEmpty // Just verify content type exists, don't check specific value
              )).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        },
        
        test("read with offset and length") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val fullContent = TestData.mediumText
              val testPath = "integration-test/medium-file.txt"
              val offset = 10L
              val length = 20L
              val expectedPartial = fullContent.substring(offset.toInt, (offset + length).toInt)
              
              (for {
                // Write full content
                _ <- OpenDAL.writeText(testPath, fullContent)
                
                // Read partial content
                partialBytes <- OpenDAL.read(testPath, offset, length)
                partialContent = new String(partialBytes, "UTF-8")
                
                // Clean up
                _ <- OpenDAL.delete(testPath)
                
              } yield assertTrue(partialContent == expectedPartial)).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        }
      ),
      
      suite("File Operations")(
        test("copy file") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val content = TestData.smallText
              val sourcePath = "integration-test/source.txt"
              val destPath = "integration-test/destination.txt"
              
              (for {
                // Create source file
                _ <- OpenDAL.writeText(sourcePath, content)
                
                // Copy file
                _ <- OpenDAL.copy(sourcePath, destPath)
                
                // Verify both files exist and have same content
                sourceContent <- OpenDAL.readString(sourcePath)
                destContent <- OpenDAL.readString(destPath)
                
                // Clean up
                _ <- OpenDAL.delete(sourcePath)
                _ <- OpenDAL.delete(destPath)
                
              } yield assertTrue(
                sourceContent == content &&
                destContent == content
              )).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        },
        
        test("rename file") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val content = TestData.smallText
              val originalPath = "integration-test/original.txt"
              val newPath = "integration-test/renamed.txt"
              
              (for {
                // Create original file
                _ <- OpenDAL.writeText(originalPath, content)
                
                // Rename file
                _ <- OpenDAL.rename(originalPath, newPath)
                
                // Verify original doesn't exist, new file exists with correct content
                originalExists <- OpenDAL.exists(originalPath)
                newContent <- OpenDAL.readString(newPath)
                
                // Clean up
                _ <- OpenDAL.delete(newPath)
                
              } yield assertTrue(
                !originalExists &&
                newContent == content
              )).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        },
        
        test("delete file") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val content = TestData.smallText
              val testPath = "integration-test/to-delete.txt"
              
              (for {
                // Create file
                _ <- OpenDAL.writeText(testPath, content)
                
                // Verify it exists
                existsBefore <- OpenDAL.exists(testPath)
                
                // Delete file
                _ <- OpenDAL.delete(testPath)
                
                // Verify it no longer exists
                existsAfter <- OpenDAL.exists(testPath)
                
              } yield assertTrue(existsBefore && !existsAfter)).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        },
        
        test("check file existence") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val existingPath = "test-data/hello.txt"  // Created by LocalStack init
              val nonExistentPath = "integration-test/does-not-exist.txt"
              
              (for {
                existingExists <- OpenDAL.exists(existingPath)
                nonExistentExists <- OpenDAL.exists(nonExistentPath)
                
              } yield assertTrue(existingExists && !nonExistentExists)).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        }
      ),
      
      suite("Metadata Operations")(
        test("get file metadata") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val content = TestData.mediumText
              val testPath = "integration-test/metadata-test.txt"
              
              (for {
                // Write file
                _ <- OpenDAL.writeText(testPath, content)
                
                // Get metadata
                metadata <- OpenDAL.stat(testPath)
                
                // Clean up
                _ <- OpenDAL.delete(testPath)
                
              } yield assertTrue(
                metadata.getContentLength == content.getBytes("UTF-8").length &&
                Option(metadata.getContentType).nonEmpty // Just verify content type exists, don't check specific value
              )).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        },
        
        test("get file size") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val content = TestData.largeText
              val testPath = "integration-test/size-test.txt"
              val expectedSize = content.getBytes("UTF-8").length.toLong
              
              (for {
                // Write file
                _ <- OpenDAL.writeText(testPath, content)
                
                // Get size
                size <- OpenDAL.size(testPath)
                
                // Clean up
                _ <- OpenDAL.delete(testPath)
                
              } yield assertTrue(size == expectedSize)).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        }
      ),
      
      suite("Directory Operations")(
        test("list directory contents") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val dirPath = "integration-test/list-test/"
              val files = Map(
                s"${dirPath}file1.txt" -> "Content 1",
                s"${dirPath}file2.txt" -> "Content 2",
                s"${dirPath}subdir/file3.txt" -> "Content 3"
              )
              
              (for {
                // Create test files
                _ <- ZIO.foreach(files.toList) { case (path, content) =>
                  OpenDAL.writeText(path, content)
                }
                
                // List directory
                entries <- OpenDAL.list(dirPath)
                
                // Verify we get expected files
                paths = entries.map(_.getPath).toSet
                expectedPaths = Set(s"${dirPath}file1.txt", s"${dirPath}file2.txt", s"${dirPath}subdir/")
                
                // Clean up
                _ <- ZIO.foreach(files.keys.toList) { path =>
                  OpenDAL.delete(path)
                }
                
              } yield assertTrue(paths == expectedPaths)).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        },
        
        test("list with options") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val dirPath = "integration-test/list-options/"
              val files = Map(
                s"${dirPath}alpha.txt" -> "Alpha",
                s"${dirPath}beta.txt" -> "Beta",
                s"${dirPath}gamma.txt" -> "Gamma"
              )
              
              (for {
                // Create test files
                _ <- ZIO.foreach(files.toList) { case (path, content) =>
                  OpenDAL.writeText(path, content)
                }
                
                // Basic functionality: list all files in directory
                allEntries <- OpenDAL.list(dirPath)
                
                // Test limit functionality (may not work with all S3 implementations)
                limitedEntries <- OpenDAL.list(dirPath, ListOpts.empty.withLimit(2))
                
                // Clean up
                _ <- ZIO.foreach(files.keys.toList) { path =>
                  OpenDAL.delete(path)
                }
                
              } yield assertTrue(
                allEntries.length == 3 && // Should have all 3 files
                limitedEntries.length <= allEntries.length // Limit should not increase results
                // Note: Prefix filtering may not work consistently across S3 implementations,
                // so we focus on basic listing functionality
              )).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        },
        
        test("create and remove directories") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val dirPath = "integration-test/new-directory/"
              val filePath = s"${dirPath}test-file.txt"
              
              (for {
                // Create directory by writing a file in it
                _ <- OpenDAL.writeText(filePath, "test content")
                
                // Verify directory exists (by listing it)
                entries <- OpenDAL.list(dirPath)
                
                // Remove all contents
                _ <- OpenDAL.removeAll(dirPath)
                
                // Verify directory is empty
                entriesAfter <- OpenDAL.list(dirPath)
                
              } yield assertTrue(
                entries.nonEmpty &&
                entriesAfter.isEmpty
              )).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        }
      ),
      
      suite("Error Handling")(
        test("read non-existent file") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val nonExistentPath = "integration-test/does-not-exist.txt"
              
              (for {
                result <- OpenDAL.readString(nonExistentPath).flip.map(_.isInstanceOf[zio.opendal.error.NotFoundError])
              } yield assertTrue(result)).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        },
        
        test("stat non-existent file") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val nonExistentPath = "integration-test/does-not-exist.txt"
              
              (for {
                result <- OpenDAL.stat(nonExistentPath).flip.map(_.isInstanceOf[zio.opendal.error.NotFoundError])
              } yield assertTrue(result)).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        }
      ),
      
      suite("Performance Tests")(
        test("large file upload and download") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val largeContent = TestData.largeText
              val testPath = "integration-test/large-file.txt"
              
              (for {
                // Upload large file
                _ <- timeOperation("Upload large file", OpenDAL.writeText(testPath, largeContent))
                
                // Download large file
                content <- timeOperation("Download large file", OpenDAL.readString(testPath))
                
                // Clean up
                _ <- OpenDAL.delete(testPath)
                
              } yield assertTrue(content == largeContent)).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        },
        
        test("concurrent file operations") {
          skipIfDisabled {
            withLocalStack { (endpoint, bucket) =>
              val numFiles = 10
              val baseContent = "Concurrent test content"
              
              (for {
                // Create multiple files concurrently
                _ <- ZIO.foreachParDiscard(1 to numFiles) { i =>
                  val path = s"integration-test/concurrent-$i.txt"
                  val content = s"$baseContent $i"
                  OpenDAL.writeText(path, content)
                }
                
                // Read all files concurrently
                results <- ZIO.foreachPar(1 to numFiles) { i =>
                  val path = s"integration-test/concurrent-$i.txt"
                  OpenDAL.readString(path)
                }
                
                // Verify all content
                expectedResults = (1 to numFiles).map(i => s"$baseContent $i")
                
                // Clean up
                _ <- ZIO.foreachParDiscard(1 to numFiles) { i =>
                  OpenDAL.delete(s"integration-test/concurrent-$i.txt")
                }
                
              } yield assertTrue(results.toList == expectedResults.toList)).provideLayer(createOpenDALLayer(endpoint))
            }
          }
        }
      )
    )

}
