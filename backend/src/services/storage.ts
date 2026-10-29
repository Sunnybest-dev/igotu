import AWS from 'aws-sdk';

const s3 = new AWS.S3({ region: process.env.AWS_REGION });
const BUCKET = process.env.RECORDINGS_BUCKET!;

/**
 * Generate a pre-signed URL valid for 15 minutes.
 * Never expose the raw S3 key or a public URL.
 */
export async function generateSignedUrl(s3Key: string): Promise<string> {
  return s3.getSignedUrlPromise('getObject', {
    Bucket: BUCKET,
    Key: s3Key,
    Expires: 900 // 15 minutes
  });
}

export async function deleteObject(s3Key: string): Promise<void> {
  await s3.deleteObject({ Bucket: BUCKET, Key: s3Key }).promise();
}

/**
 * Generate a pre-signed PUT URL so the Android client can upload
 * directly to S3 without routing the binary through the API server.
 */
export async function generateUploadUrl(s3Key: string): Promise<string> {
  return s3.getSignedUrlPromise('putObject', {
    Bucket: BUCKET,
    Key: s3Key,
    Expires: 3600,
    ContentType: 'video/mp4',
    ServerSideEncryption: 'AES256'
  });
}
