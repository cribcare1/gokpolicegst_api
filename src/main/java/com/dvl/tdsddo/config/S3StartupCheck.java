//package com.dvl.tdsddo.config;
//
//import jakarta.annotation.PostConstruct;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.*;
//
//import java.util.List;
//
//@Component
//public class S3StartupCheck {
//
//	private final S3Client s3Client;
//
//	@Value("${aws.s3.bucket}")
//	private String bucketName;
//
//	public S3StartupCheck(S3Client s3Client) {
//		this.s3Client = s3Client;
//	}
//
//	@PostConstruct
//	public void printBucketDetails() {
//		try {
//			// ✅ Check if the bucket exists by calling HeadBucket
//			HeadBucketRequest headBucketRequest = HeadBucketRequest.builder().bucket(bucketName).build();
//			s3Client.headBucket(headBucketRequest);
//			System.out.println("✅ Bucket exists: " + bucketName);
//
//			// ✅ List objects in the bucket
//			ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(bucketName).maxKeys(10) // limit to
//																												// 10
//																												// objects
//					.build();
//			ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
//			List<S3Object> objects = listResponse.contents();
//
//			System.out.println("📦 Objects in bucket:");
//			for (S3Object obj : objects) {
//				System.out.println("- " + obj.key() + " (" + obj.size() + " bytes)");
//			}
//
//		} catch (S3Exception e) {
//			System.err.println("❌ S3 error for check up : " + e.awsErrorDetails().errorMessage());
//			System.err.println("❌ HTTP Status Code: " + e.statusCode());
//			System.err.println("❌ AWS Error Code: " + e.awsErrorDetails().errorCode());
//			System.err.println("❌ Request ID: " + e.requestId());
//			throw e; // Optional: rethrow if you want app to crash on failure
//		}
//	}
//
//	@PostConstruct
//	public void printBucketDetailsS() {
//		try {
//			System.out.println("Bucket Name: " + bucketName);
//			System.out.println("S3 Client: " + s3Client);
//
//			// ✅ Check if the bucket exists
//			HeadBucketRequest headBucketRequest = HeadBucketRequest.builder().bucket(bucketName).build();
//			s3Client.headBucket(headBucketRequest);
//			System.out.println("✅ Bucket exists: " + bucketName);
//
//			// ✅ List objects in the bucket
//			ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(bucketName).maxKeys(10).build();
//			ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
//
//			List<S3Object> objects = listResponse.contents();
//			System.out.println("📦 Objects in bucket:");
//			for (S3Object obj : objects) {
//				System.out.println("- " + obj.key() + " (" + obj.size() + " bytes)");
//			}
//
//		} catch (NoSuchBucketException e) {
//			System.err.println("❌ Bucket does not exist: " + bucketName);
//			e.printStackTrace();
//		} catch (S3Exception e) {
//			System.err.println("❌ S3 error during startup: " + e.awsErrorDetails().errorMessage());
//			e.printStackTrace();
//		} catch (Exception e) {
//			System.err.println("❌ General error during S3 startup check: " + e.getMessage());
//			e.printStackTrace();
//		}
//	}
//
//}
