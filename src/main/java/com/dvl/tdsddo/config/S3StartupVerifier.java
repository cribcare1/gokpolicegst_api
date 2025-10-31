package com.dvl.tdsddo.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@RequiredArgsConstructor
public class S3StartupVerifier {

	private final S3Client s3Client;

//	@PostConstruct
	public void verifyS3Connection() {
		try {
			ListBucketsResponse bucketsResponse = s3Client.listBuckets();
			System.out.println("✅ S3 Configuration successful. Buckets found:");
			bucketsResponse.buckets().forEach(bucket -> System.out.println(" - " + bucket.name()));
		} catch (S3Exception e) {
			System.err.println("❌ Failed to connect to S3: " + e.awsErrorDetails().errorMessage());
		} catch (Exception ex) {
			System.err.println("❌ Unexpected error while connecting to S3: " + ex.getMessage());
		}
	}

//	@PostConstruct
	public void verifyS3Access() {
		try {
			String testBucket = "tds-bucket-storage";

			// List objects instead of listing all buckets
			ListObjectsV2Response list = s3Client.listObjectsV2(builder -> builder.bucket(testBucket));
			System.out.println(
					"✅ Access to S3 bucket '" + testBucket + "' is working. Found " + list.keyCount() + " objects.");
		} catch (S3Exception e) {
			System.err.println("❌ S3 Access Error: " + e.awsErrorDetails().errorMessage());
		}
	}

}
