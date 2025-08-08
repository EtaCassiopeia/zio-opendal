#!/bin/bash

# ZIO OpenDAL Integration Test Runner
# This script sets up and runs integration tests using LocalStack

set -e

echo "=== ZIO OpenDAL Integration Test Runner ==="

# Configuration
LOCALSTACK_IMAGE="localstack/localstack:3.9.0"
CONTAINER_NAME="zio-opendal-localstack-test"
BUCKET_NAME="zio-opendal-test-bucket"
REGION="us-east-1"

# Function to cleanup on exit
cleanup() {
    echo "Cleaning up..."
    docker stop $CONTAINER_NAME >/dev/null 2>&1 || true
    docker rm $CONTAINER_NAME >/dev/null 2>&1 || true
    echo "Cleanup completed"
}

# Set trap to cleanup on exit
trap cleanup EXIT

# Check if Docker is running
if ! docker info >/dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

echo "Starting LocalStack container..."

# Start LocalStack
docker run -d \
    --name $CONTAINER_NAME \
    -p 4566:4566 \
    -e DEBUG=1 \
    -e SERVICES=s3,iam \
    -e AWS_DEFAULT_REGION=$REGION \
    -e AWS_ACCESS_KEY_ID=test \
    -e AWS_SECRET_ACCESS_KEY=test \
    -e S3_SKIP_SIGNATURE_VALIDATION=1 \
    -e S3_FORCE_PATH_STYLE=1 \
    -e LOCALSTACK_HOST=localhost \
    -e EDGE_PORT=4566 \
    -e PERSISTENCE=0 \
    --health-cmd="curl -f http://localhost:4566/_localstack/health" \
    --health-interval=30s \
    --health-timeout=10s \
    --health-retries=5 \
    --health-start-period=30s \
    $LOCALSTACK_IMAGE

echo "Waiting for LocalStack to be ready..."

# Wait for LocalStack to be ready
timeout 60s bash -c 'until docker exec '$CONTAINER_NAME' curl -f http://localhost:4566/_localstack/health; do sleep 2; done'

echo "LocalStack is ready! Setting up S3 bucket..."

# Setup S3 bucket
docker exec $CONTAINER_NAME aws --endpoint-url=http://localhost:4566 s3api create-bucket \
    --bucket $BUCKET_NAME \
    --region $REGION \
    --create-bucket-configuration LocationConstraint=$REGION

# Verify bucket creation
echo "Verifying bucket creation..."
docker exec $CONTAINER_NAME aws --endpoint-url=http://localhost:4566 s3 ls

echo "✅ LocalStack setup completed!"
echo ""
echo "Environment ready:"
echo "  - LocalStack endpoint: http://localhost:4566"
echo "  - S3 bucket: $BUCKET_NAME"
echo "  - AWS credentials: test/test"
echo ""

# Set environment variables for tests
export ENABLE_INTEGRATION_TESTS=true
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=$REGION
export S3_ENDPOINT_URL=http://localhost:4566
export OPENDAL_S3_ENDPOINT=http://localhost:4566
export OPENDAL_S3_REGION=$REGION
export OPENDAL_S3_ACCESS_KEY_ID=test
export OPENDAL_S3_SECRET_ACCESS_KEY=test
export OPENDAL_S3_BUCKET=$BUCKET_NAME
export AWS_S3_FORCE_PATH_STYLE=true

echo "Running integration tests..."
echo "Command: sbt IntegrationTest/test"
echo ""

# Run integration tests
if sbt IntegrationTest/test; then
    echo ""
    echo "✅ Integration tests completed successfully!"
else
    echo ""
    echo "❌ Integration tests failed!"
    exit 1
fi

echo ""
echo "=== Integration test run completed ==="
