#!/bin/bash

# LocalStack S3 setup script for ZIO OpenDAL integration tests
# This script runs when LocalStack becomes ready

set -e

echo "=== Setting up LocalStack S3 for ZIO OpenDAL Integration Tests ==="

# Configuration
BUCKET_NAME="zio-opendal-test-bucket"
REGION="us-east-1"
AWS_ENDPOINT="http://localhost:4566"

# Create S3 bucket for testing
echo "Creating S3 bucket: $BUCKET_NAME"
aws --endpoint-url=$AWS_ENDPOINT s3api create-bucket \
    --bucket $BUCKET_NAME \
    --region $REGION \
    --create-bucket-configuration LocationConstraint=$REGION 2>/dev/null || echo "Bucket already exists"

# Enable bucket versioning (optional, for advanced tests)
echo "Enabling versioning on bucket: $BUCKET_NAME"
aws --endpoint-url=$AWS_ENDPOINT s3api put-bucket-versioning \
    --bucket $BUCKET_NAME \
    --versioning-configuration Status=Enabled

# Set up bucket CORS configuration for web access (if needed)
echo "Setting up CORS configuration"
aws --endpoint-url=$AWS_ENDPOINT s3api put-bucket-cors \
    --bucket $BUCKET_NAME \
    --cors-configuration '{
        "CORSRules": [
            {
                "AllowedOrigins": ["*"],
                "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
                "AllowedHeaders": ["*"],
                "ExposeHeaders": ["ETag", "x-amz-version-id"],
                "MaxAgeSeconds": 3600
            }
        ]
    }'

# Create some test directories/prefixes
echo "Creating test directory structure"
aws --endpoint-url=$AWS_ENDPOINT s3api put-object \
    --bucket $BUCKET_NAME \
    --key "test-data/"

aws --endpoint-url=$AWS_ENDPOINT s3api put-object \
    --bucket $BUCKET_NAME \
    --key "integration-tests/"

aws --endpoint-url=$AWS_ENDPOINT s3api put-object \
    --bucket $BUCKET_NAME \
    --key "temp/"

# Add some sample test files
echo "Adding sample test files"
echo "Hello, ZIO OpenDAL!" | aws --endpoint-url=$AWS_ENDPOINT s3 cp - s3://$BUCKET_NAME/test-data/hello.txt
echo '{"message": "Integration test data", "timestamp": "2024-01-01T00:00:00Z"}' | aws --endpoint-url=$AWS_ENDPOINT s3 cp - s3://$BUCKET_NAME/test-data/sample.json

# Verify setup
echo "Verifying S3 setup..."
aws --endpoint-url=$AWS_ENDPOINT s3 ls s3://$BUCKET_NAME/

echo "=== LocalStack S3 setup completed successfully ==="
echo "Bucket: $BUCKET_NAME"
echo "Region: $REGION"
echo "Endpoint: $AWS_ENDPOINT"
echo "Access Key: test"
echo "Secret Key: test"
