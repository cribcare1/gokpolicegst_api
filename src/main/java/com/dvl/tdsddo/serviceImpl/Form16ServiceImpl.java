package com.dvl.tdsddo.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.Form16;
import com.dvl.tdsddo.model.User;
import com.dvl.tdsddo.repository.Form16Repository;
import com.dvl.tdsddo.repository.UserRepository;
import com.dvl.tdsddo.request.Form16Request;
import com.dvl.tdsddo.response.DashBoardresponse;
import com.dvl.tdsddo.response.FileDownloadResponse;
import com.dvl.tdsddo.response.Form16Response;
import com.dvl.tdsddo.service.Form16Service;
import com.dvl.tdsddo.service.UserService;
import com.dvl.tdsddo.util.FileServiceUtil;
import com.dvl.tdsddo.util.FolderCreatorBean;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class Form16ServiceImpl implements Form16Service {

	@Autowired
	private S3Client s3Client;
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private Form16Repository form16Repository;

	@Autowired
	private UserService userService;

	@Autowired
	private FolderCreatorBean folderCreatorBean;

	@Autowired
	private FileServiceUtil fileStorageService;
	@Value("${folder.user.document.path}")
	private String documentPath;

//	private static final String BASE_UPLOAD_PATH = "C:/uploads/tdsddo/user/financialyear/";
	private static final String BASE_UPLOAD_PATH = "/home3/ewingstds/public_html/uploads";

	@Value("${folder.user.document.financialYear}")
	private String basePath;

	@Value("${base.download.url}")
	private String baseUrl;

	@Value("${aws.s3.bucket}")
	private String bucketName;

	@PostConstruct
	public void printObj() {
		System.err.println(s3Client);
	}

	@Override
	public Map<String, Object> uploadForm16(String form16Request, MultipartFile file) {
		Map<String, Object> response = new HashMap<>();

		try {
			// Convert form16Request JSON string to Form16Request object (assuming JSON
			// format)
			ObjectMapper objectMapper = new ObjectMapper();
			Form16Request request = objectMapper.readValue(form16Request, Form16Request.class);

			// Validate DDO
			Optional<User> ddoUser = userRepository.findById(request.getDdoId());
			if (ddoUser.isEmpty()) {
				response.put(TdsDdoConstant.STATUS, TdsDdoConstant.ERROR);
				response.put(TdsDdoConstant.MESSAGE, "Invalid DDO ID");
				return response;
			}

			// Check if Form16 already exists for the given PAN number and Financial Year
			Optional<Form16> existingForm16 = form16Repository.findByPanNumberAndFinancialYear(request.getPanNumber(),
					request.getFinancialYear());

			// Upload file and get the file path
			String fileName = fileStorageService.uploadFile(file, request.getName(), request.getPanNumber(),
					request.getFinancialYear());

			if (fileName == null) {
				response.put(TdsDdoConstant.STATUS, TdsDdoConstant.ERROR);
				response.put(TdsDdoConstant.MESSAGE, "File upload failed");
				return response;
			}

			Form16 form16;
			if (existingForm16.isPresent()) {
				// Update existing record
				form16 = existingForm16.get();
				form16.setFilePath(fileName); // Update file path

				response.put(TdsDdoConstant.MESSAGE, "Form 16 updated successfully");
			} else {
				// Create new record
				form16 = Form16.builder().ddo(ddoUser.get()).financialYear(request.getFinancialYear())
						.name(request.getName()).panNumber(request.getPanNumber())
						.mobileNumber(request.getMobileNumber()).email(request.getEmail()).filePath(fileName).build();

				response.put("message", "Form 16 uploaded successfully");
			}

			// Save to the database
			form16Repository.save(form16);

			response.put(TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS);
			return response;

		} catch (IOException e) {
			response.put(TdsDdoConstant.STATUS, TdsDdoConstant.ERROR);
			response.put(TdsDdoConstant.MESSAGE, "Error uploading file: " + e.getMessage());
			return response;
		}
	}

	@Override
	public Map<String, Object> getLastThreeYearsForm16(String panNumber) {
		try {
			Pageable pageable = PageRequest.of(0, 3); // Fetch only the last 3 records
			List<Form16Response> response = form16Repository.findTop3ByPanNumber(panNumber, pageable);
			if (response != null && response.isEmpty()) {
				return Map.of(TdsDdoConstant.MESSAGE, "There are no form 16 present for this perticular user.",
						TdsDdoConstant.STATUS, TdsDdoConstant.ERROR);
			}
			String baseUrl = "http://13.126.232.163:8888";
			response.forEach(form16 -> {
				String downloadLink = baseUrl + "/tds/form16/downloadPDF/" + form16.getForm16FilePath();
				form16.setForm16FilePath(downloadLink);
			});
			return Map.of(TdsDdoConstant.MESSAGE, "List of documents.", TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS,
					TdsDdoConstant.FORM16_List, response);
		} catch (Exception e) {
			return Map.of(TdsDdoConstant.MESSAGE, e.getMessage(), TdsDdoConstant.STATUS, TdsDdoConstant.ERROR);
		}
	}

	@Override
	public Map<String, Object> generateDownloadLink(String fileName) {
		try {
			Path filePath = Paths.get(documentPath).resolve(fileName).normalize();
			if (!Files.exists(filePath)) {
				return Map.of("message", "There are no such file.", "status", "error");
			}
			String baseUrl = "http://localhost:8888";
			String downloadLink = baseUrl + "/tds/form16/downloadPDF/" + fileName;

			return Map.of(TdsDdoConstant.MESSAGE, "Download link generated", TdsDdoConstant.STATUS,
					TdsDdoConstant.SUCCESS, "downloadLink", downloadLink);
		} catch (Exception e) {
			return Map.of(TdsDdoConstant.MESSAGE, "Error generating download link!", TdsDdoConstant.STATUS,
					TdsDdoConstant.ERROR);
		}
	}

	@Override
	public ResponseEntity<Resource> downloadPDF(String fileName) {
		try {
			Path filePath = Paths.get(documentPath).resolve(fileName).normalize();
			if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
			}
			Resource resource = new UrlResource(filePath.toUri());

			if (!resource.exists() || !resource.isReadable()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
			}

			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
					.contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);

		} catch (MalformedURLException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	@Override
	public ResponseEntity<Resource> downloadPDFNew(String fileName) {
		try {
			Path filePath = Paths.get(basePath).resolve(fileName).normalize();
			if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
			}
			Resource resource = new UrlResource(filePath.toUri());

			if (!resource.exists() || !resource.isReadable()) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
			}

			return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
					.contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);

		} catch (MalformedURLException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
	}

	@Override
	public List<FileDownloadResponse> getForm16Files(String financialYear, String panNumber) {
		List<FileDownloadResponse> responseList = new ArrayList<>();

		File folder = new File(basePath + financialYear);
		if (!folder.exists() || !folder.isDirectory()) {
			return responseList; // return empty list if folder doesn't exist
		}

		File[] files = folder.listFiles(
				(dir, name) -> name.toLowerCase().startsWith(panNumber.toLowerCase()) && name.endsWith(".pdf"));

		if (files != null) {
			for (File file : files) {
				String downloadLink = baseUrl + financialYear + "/" + file.getName();
				responseList.add(new FileDownloadResponse(file.getName(), downloadLink));
			}
		}

		return responseList;
	}

	@Override
	public Map<String, Object> uploadForm16Files(String financialYear, String tanNumber, String type,
			MultipartFile[] files) {
		Map<String, Object> response = new HashMap<>();

		try {
			if (!type.equalsIgnoreCase("form16") && !type.equalsIgnoreCase("form16A")) {
				return Map.of("message", "Invalid type. Must be 'form16' or 'form16A'", "status", "error");
			}

			Path targetDir = Paths.get(BASE_UPLOAD_PATH, financialYear, type.toLowerCase(), tanNumber);

			if (!Files.exists(targetDir)) {
				Files.createDirectories(targetDir);
			}

			for (MultipartFile file : files) {
				Path filePath = targetDir.resolve(file.getOriginalFilename());
				Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
			}
			DashBoardresponse dashBoardresponse = userService.viewDashBoard(null);
			if (type != null && type.equalsIgnoreCase("form16")) {
				response.put("form16Count", dashBoardresponse.getForm16Count());
			}
			if (type != null && type.equalsIgnoreCase("form16a")) {
				response.put("form16ACount", dashBoardresponse.getForm16ACount());
			}

			response.put("message", "Files uploaded successfully");
			response.put("status", "success");
		} catch (Exception e) {
			response.put("message", "Upload failed: " + e.getMessage());
			response.put("status", "error");
		}

		return response;
	}

	// TODO get upload forms for local drive
//	@Override
//	public Map<String, Object> getUploadedFormNames(String financialYear, String tanNumber, String type) {
//		Map<String, Object> response = new HashMap<>();
//
//		try {
//			// Validate type
//			if (!type.equalsIgnoreCase("form16") && !type.equalsIgnoreCase("form16A")) {
//				return Map.of("message", "Invalid form type", "status", "error");
//			}
//
//			// Construct directory path
//			Path dirPath = Paths.get(BASE_UPLOAD_PATH, financialYear, type.toLowerCase(), tanNumber);
//
//			// Check if directory exists
//			if (!Files.exists(dirPath)) {
//				return Map.of("message", "There is no form16 available. Please contact admin to upload.", "status",
//						"error");
//			}
//
//			// Get list of file names
//			List<String> fileNames = Files.list(dirPath).filter(Files::isRegularFile)
//					.map(path -> path.getFileName().toString()).collect(Collectors.toList());
//
//			response.put("status", "success");
//			response.put("message", "file list retrieved.");
//			response.put("files", fileNames);
//		} catch (Exception e) {
//			response.put("status", "error");
//			response.put("message", "Failed to retrieve file list: " + e.getMessage());
//		}
//
//		return response;
//	}
//	@Override
//	public Map<String, Object> getUploadedFormNames(String financialYear, String tanNumber, String type) {
//		Map<String, Object> response = new HashMap<>();
//
//		try {
//			// Validate type
//			if (!type.equalsIgnoreCase("form16") && !type.equalsIgnoreCase("form16A")) {
//				return Map.of("message", "Invalid form type", "status", "error");
//			}
//
//			// Build the S3 "folder" path (prefix)
//			String prefix = financialYear + "/" + type.toLowerCase() + "/" + tanNumber + "/";
//
//			// List objects under the prefix
//			ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix).build();
//
//			ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
//
//			List<String> fileNames = listResponse.contents().stream().filter(s3Object -> !s3Object.key().endsWith("/")) // ignore
//																														// "folder
//																														// markers"
//					.map(s3Object -> s3Object.key().substring(prefix.length())) // Remove prefix to get file name only
//					.collect(Collectors.toList());
//
//			if (fileNames.isEmpty()) {
//				return Map.of("message", "There is no form16 available. Please contact admin to upload.", "status",
//						"error");
//			}
//
//			response.put("status", "success");
//			response.put("message", "File list retrieved.");
//			response.put("files", fileNames);
//		} catch (Exception e) {
//			response.put("status", "error");
//			response.put("message", "Failed to retrieve file list: " + e.getMessage());
//		}
//
//		return response;
//	}

	@Override
	public Map<String, Object> getUploadedFormNames(String financialYear, String tanNumber, String type) {
		Map<String, Object> response = new HashMap<>();

		try {
			// Validate type
			if (!type.equalsIgnoreCase("form16") && !type.equalsIgnoreCase("form16a")) {
				return Map.of("message", "Invalid form type", "status", "error");
			}

			// Build prefix
			String prefix = financialYear + "/" + type.toLowerCase() + "/" + tanNumber + "/";

			List<String> fileNames = new ArrayList<>();
			String continuationToken = null;

			do {
				ListObjectsV2Request.Builder listRequestBuilder = ListObjectsV2Request.builder()
						.bucket(bucketName)
						.prefix(prefix)
						.maxKeys(1000);

				if (continuationToken != null) {
					listRequestBuilder.continuationToken(continuationToken);
				}

				ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequestBuilder.build());

				// Filter out folder markers and get filenames relative to prefix
				listResponse.contents().stream()
						.filter(s3Object -> !s3Object.key().endsWith("/"))
						.map(s3Object -> s3Object.key().substring(prefix.length()))
						.forEach(fileNames::add);

				continuationToken = listResponse.nextContinuationToken();
			} while (continuationToken != null);

			if (fileNames.isEmpty()) {
				return Map.of("message", "There is no form16 available. Please contact admin to upload.", "status", "error");
			}

			response.put("status", "success");
			response.put("message", "File list retrieved.");
			response.put("files", fileNames);

		} catch (Exception e) {
			response.put("status", "error");
			response.put("message", "Failed to retrieve file list: " + e.getMessage());
		}

		return response;
	}


//	@Override
//	public Map<String, Object> getUploadedFormNames(String financialYear, String tanNumber, String type) {
//		Map<String, Object> response = new HashMap<>();
//
//		String ftpServer = "ftp.ewingstds.com";
//		int ftpPort = 21;
//		String ftpUser = "ewingstds";
//		String ftpPass = "TDSwings#%26";
//		String remotePath = "/public_html/uploads/" + financialYear + "/" + type.toLowerCase() + "/" + tanNumber;
//
//		FTPClient ftpClient = new FTPClient();
//
//		try {
//			// Validate type
//			if (!type.equalsIgnoreCase("form16") && !type.equalsIgnoreCase("form16A")) {
//				return Map.of("message", "Invalid form type", "status", "error");
//			}
//
//			// Connect and login
//			ftpClient.connect(ftpServer, ftpPort);
//			ftpClient.login(ftpUser, ftpPass);
//			ftpClient.enterLocalPassiveMode();
//
//			// Change to the desired directory
//			boolean dirExists = ftpClient.changeWorkingDirectory(remotePath);
//			if (!dirExists) {
//				return Map.of("message", "There is no form16 available. Please contact admin to upload.", "status",
//						"error");
//			}
//
//			// List files in directory
//			FTPFile[] files = ftpClient.listFiles();
//			List<String> fileNames = Arrays.stream(files).filter(FTPFile::isFile).map(FTPFile::getName)
//					.collect(Collectors.toList());
//
//			response.put("status", "success");
//			response.put("message", "file list retrieved.");
//			response.put("files", fileNames);
//
//		} catch (Exception e) {
//			response.put("status", "error");
//			response.put("message", "Failed to retrieve file list: " + e.getMessage());
//		} finally {
//			try {
//				if (ftpClient.isConnected()) {
//					ftpClient.logout();
//					ftpClient.disconnect();
//				}
//			} catch (IOException ignored) {
//			}
//		}
//
//		return response;
//	}

//	@Override
//	public ResponseEntity<Resource> downloadFormsAsZip(String financialYear, String tanNumber, String type,
//			List<String> fileNames) {
//		try {
//			if (!type.equalsIgnoreCase("form16") && !type.equalsIgnoreCase("form16A")) {
//				return ResponseEntity.badRequest().body(null);
//			}
//
//			String basePath = "C:/uploads/tdsddo/user/financialyear/";
//			Path dirPath = Paths.get(basePath, financialYear, type.toLowerCase(), tanNumber);
//
//			// Create temp zip file
//			Path zipPath = Files.createTempFile("form_download_", ".zip");
//
//			try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipPath))) {
//				for (String fileName : fileNames) {
//					Path filePath = dirPath.resolve(fileName);
//					if (Files.exists(filePath)) {
//						zipOut.putNextEntry(new ZipEntry(fileName));
//						Files.copy(filePath, zipOut);
//						zipOut.closeEntry();
//					}
//				}
//			}
//
//			InputStream inputStream = Files.newInputStream(zipPath);
//			Resource resource = new InputStreamResource(inputStream);
//
//			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=forms.zip")
//					.contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
//
//		} catch (Exception e) {
//			return ResponseEntity.internalServerError().body(null);
//		}
//	}

	// TODO this is used for down load the file for file system.
//	@Override
//	public ResponseEntity<Resource> downloadFormsAsZip(String financialYear, String tanNumber, String type,
//			List<String> fileNames) {
//		try {
//			if (!type.equalsIgnoreCase("form16") && !type.equalsIgnoreCase("form16A")) {
//				return ResponseEntity.badRequest().body(null);
//			}
//
//			String basePath = "C:/uploads/tdsddo/user/financialyear/";
//			Path dirPath = Paths.get(basePath, financialYear, type.toLowerCase(), tanNumber);
//
//			if (fileNames.size() == 1) {
//				// If only one file, return it directly as PDF
//				Path filePath = dirPath.resolve(fileNames.get(0));
//				if (Files.exists(filePath)) {
//					Resource resource = new InputStreamResource(Files.newInputStream(filePath));
//					return ResponseEntity.ok()
//							.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileNames.get(0))
//							.contentType(MediaType.APPLICATION_PDF).body(resource);
//				} else {
//					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//				}
//			}
//
//			// Otherwise zip and return
//			Path zipPath = Files.createTempFile("form_download_", ".zip");
//
//			try (ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipPath))) {
//				for (String fileName : fileNames) {
//					Path filePath = dirPath.resolve(fileName);
//					if (Files.exists(filePath)) {
//						zipOut.putNextEntry(new ZipEntry(fileName));
//						Files.copy(filePath, zipOut);
//						zipOut.closeEntry();
//					}
//				}
//			}
//
//			Resource resource = new InputStreamResource(Files.newInputStream(zipPath));
//
//			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=forms.zip")
//					.contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
//
//		} catch (Exception e) {
//			return ResponseEntity.internalServerError().body(null);
//		}
//	}

//	@Override
//	public ResponseEntity<Resource> downloadFormsAsZip(String financialYear, String tanNumber, String type,
//			List<String> fileNames) {
//		String ftpServer = "ftp.ewingstds.com";
//		int ftpPort = 21;
//		String ftpUser = "ewingstds";
//		String ftpPass = "TDSwings#%26";
//		String remotePath = "/public_html/uploads/" + financialYear + "/" + type.toLowerCase() + "/" + tanNumber;
//
//		FTPClient ftpClient = new FTPClient();
//
//		try {
//			ftpClient.connect(ftpServer, ftpPort);
//			ftpClient.login(ftpUser, ftpPass);
//			ftpClient.enterLocalPassiveMode();
//			ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
//
//			if (fileNames.size() == 1) {
//				// Single file download
//				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//				boolean success = ftpClient.retrieveFile(remotePath + "/" + fileNames.get(0), outputStream);
//
//				if (!success) {
//					return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
//				}
//
//				ByteArrayResource resource = new ByteArrayResource(outputStream.toByteArray());
//				return ResponseEntity.ok()
//						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileNames.get(0))
//						.contentType(MediaType.APPLICATION_PDF).body(resource);
//			}
//
//			// Multiple files → zip
//			ByteArrayOutputStream zipOutStream = new ByteArrayOutputStream();
//			try (ZipOutputStream zipOut = new ZipOutputStream(zipOutStream)) {
//				for (String fileName : fileNames) {
//					ByteArrayOutputStream fileStream = new ByteArrayOutputStream();
//					boolean success = ftpClient.retrieveFile(remotePath + "/" + fileName, fileStream);
//
//					if (success) {
//						zipOut.putNextEntry(new ZipEntry(fileName));
//						zipOut.write(fileStream.toByteArray());
//						zipOut.closeEntry();
//					}
//				}
//			}
//
//			ByteArrayResource zipResource = new ByteArrayResource(zipOutStream.toByteArray());
//
//			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=forms.zip")
//					.contentType(MediaType.APPLICATION_OCTET_STREAM).body(zipResource);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//			return ResponseEntity.internalServerError().body(null);
//		} finally {
//			try {
//				if (ftpClient.isConnected()) {
//					ftpClient.logout();
//					ftpClient.disconnect();
//				}
//			} catch (IOException ex) {
//				ex.printStackTrace();
//			}
//		}
//	}

	@Override
	public ResponseEntity<Resource> downloadFormsAsZip(String financialYear, String tanNumber, String type,
			List<String> fileNames) {
		try {
			if (!type.equalsIgnoreCase("form16") && !type.equalsIgnoreCase("form16A")) {
				return ResponseEntity.badRequest().body(null);
			}

			String basePath = financialYear + "/" + type.toLowerCase() + "/" + tanNumber + "/";

			if (fileNames.size() == 1) {
				// Only one file, download it directly
				String s3Key = basePath + fileNames.get(0);

				ResponseInputStream<GetObjectResponse> s3Object = s3Client
						.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3Key).build());

				Resource resource = new InputStreamResource(s3Object);

				return ResponseEntity.ok()
						.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileNames.get(0))
						.contentType(MediaType.APPLICATION_PDF).body(resource);
			}

			// Otherwise, zip multiple files
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
				for (String fileName : fileNames) {
					String s3Key = basePath + fileName;

					try (ResponseInputStream<GetObjectResponse> s3Object = s3Client
							.getObject(GetObjectRequest.builder().bucket(bucketName).key(s3Key).build())) {

						zipOut.putNextEntry(new ZipEntry(fileName));
						s3Object.transferTo(zipOut); // directly transfer from S3 to zip
						zipOut.closeEntry();
					} catch (Exception e) {
						// Skip missing files or error files
						System.out.println("Failed to fetch file from S3: " + fileName + ", skipping.");
					}
				}
			}

			ByteArrayResource resource = new ByteArrayResource(baos.toByteArray());

			return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=forms.zip")
					.contentType(MediaType.APPLICATION_OCTET_STREAM).contentLength(resource.contentLength())
					.body(resource);

		} catch (Exception e) {
			return ResponseEntity.internalServerError().body(null);
		}
	}

	@Override
	public Map<String, Object> searchFilesByPan(String financialYear, String tanNumber, String formType,
			String panNumber) {
		Map<String, Object> response = new HashMap<>();

		try {
			if (!formType.equalsIgnoreCase("form16") && !formType.equalsIgnoreCase("form16A")) {
				response.put("status", "error");
				response.put("message", "Invalid form type");
				return response;
			}

			String basePath = "C:/uploads/tdsddo/user/financialyear/";
			String folderPath = basePath + financialYear + "/" + formType.toLowerCase() + "/" + tanNumber;
			File folder = new File(folderPath);

			if (!folder.exists() || !folder.isDirectory()) {
				response.put("status", "error");
				response.put("message", "Directory not found");
				return response;
			}

			// Case-insensitive file search by PAN number
			File[] matchedFiles = folder.listFiles((dir, name) -> name.toLowerCase().contains(panNumber.toLowerCase())
					&& name.toLowerCase().endsWith(".pdf"));

			List<String> fileNames = new ArrayList<>();
			if (matchedFiles != null) {
				for (File file : matchedFiles) {
					fileNames.add(file.getName());
				}
			}

			response.put("status", "success");
			response.put("fileNames", fileNames);
		} catch (Exception e) {
			response.put("status", "error");
			response.put("message", "Error while searching files: " + e.getMessage());
		}

		return response;
	}

//	@Override
//	public Map<String, Object> uploadForm16FilesWithZipFolder(String financialYear, String tanNumber, String type,
//			MultipartFile zipFile) {
//		Map<String, Object> response = new HashMap<>();
//
//		try {
//			if (!type.equalsIgnoreCase("form16") && !type.equalsIgnoreCase("form16A")) {
//				return Map.of("message", "Invalid type. Must be 'form16' or 'form16A'", "status", "error");
//			}
//
//			Path targetDir = Paths.get(BASE_UPLOAD_PATH, financialYear, type.toLowerCase(), tanNumber);
//			if (!Files.exists(targetDir)) {
//				Files.createDirectories(targetDir);
//			}
//
//			// Unzip the zipFile content into the targetDir
//			try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
//				ZipEntry entry;
//				while ((entry = zis.getNextEntry()) != null) {
//					if (!entry.isDirectory()) {
//						Path filePath = targetDir.resolve(entry.getName());
//						Files.createDirectories(filePath.getParent()); // In case there are folders inside zip
//						Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
//					}
//					zis.closeEntry();
//				}
//			}
//
//			// Dashboard counts (just like your original logic)
//			DashBoardresponse dashBoardresponse = userService.viewDashBoard(null);
//			if (type.equalsIgnoreCase("form16")) {
//				response.put("form16Count", dashBoardresponse.getForm16Count());
//			} else if (type.equalsIgnoreCase("form16a")) {
//				response.put("form16ACount", dashBoardresponse.getForm16ACount());
//			}
//
//			response.put("message", "ZIP file uploaded and extracted successfully");
//			response.put("status", "success");
//		} catch (Exception e) {
//			response.put("message", "Upload failed: " + e.getMessage());
//			response.put("status", "error");
//		}
//
//		return response;
//	}

	// TODO This if for local file storage
//	@Override
//	public Map<String, Object> uploadForm16FilesWithZipFolder(String financialYear, String tanNumber, String type,
//			MultipartFile file) {
//
//		Map<String, Object> response = new HashMap<>();
//
//		// FTP server configuration
//		String ftpServer = "ftp.ewingstds.com";
//		int ftpPort = 21;
//		String ftpUser = "ewingstds";
//		String ftpPass = "TDSwings#%26";
//		String remotePath = "/public_html/uploads/" + financialYear + "/" + type.toLowerCase() + "/" + tanNumber;
//
//		try {
//			if (!type.equalsIgnoreCase("form16") && !type.equalsIgnoreCase("form16A")) {
//				return Map.of("message", "Invalid type. Must be 'form16' or 'form16A'", "status", "error");
//			}
//
//			// Create remote folder
//			folderCreatorBean.createRemoteFolder(ftpServer, ftpPort, ftpUser, ftpPass, remotePath);
//
//			String originalFileName = file.getOriginalFilename();
//			if (originalFileName == null) {
//				throw new IllegalArgumentException("File name cannot be null");
//			}
//
//			if (originalFileName.toLowerCase().endsWith(".zip")) {
//				try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
//					ZipEntry entry;
//					while ((entry = zis.getNextEntry()) != null) {
//						if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".pdf")) {
//							String fileName = Paths.get(entry.getName()).getFileName().toString();
//
//							folderCreatorBean.uploadFileToFTP(ftpServer, ftpPort, ftpUser, ftpPass, remotePath,
//									fileName, zis);
//						}
//						zis.closeEntry();
//					}
//				}
//			} else if (originalFileName.toLowerCase().endsWith(".pdf")) {
//				folderCreatorBean.uploadFileToFTP(ftpServer, ftpPort, ftpUser, ftpPass, remotePath, originalFileName,
//						file.getInputStream());
//			} else {
//				return Map.of("message", "Unsupported file type. Only PDF and ZIP are allowed", "status", "error");
//			}
//
//			// Dashboard counts
//			DashBoardresponse dashBoardresponse = userService.viewDashBoard(null);
//			if (type.equalsIgnoreCase("form16")) {
//				response.put("form16Count", dashBoardresponse.getForm16Count());
//			} else if (type.equalsIgnoreCase("form16a")) {
//				response.put("form16ACount", dashBoardresponse.getForm16ACount());
//			}
//
//			response.put("message", "File uploaded and processed successfully");
//			response.put("status", "success");
//
//		} catch (Exception e) {
//			response.put("message", "Upload failed: " + e.getMessage());
//			response.put("status", "error");
//		}
//
//		return response;
//	}

	@Override
	public Map<String, Object> uploadForm16FilesWithZipFolder(String financialYear, String tanNumber, String type,
			MultipartFile file) {

		Map<String, Object> response = new HashMap<>();

		try {
			if (!type.equalsIgnoreCase("form16") && !type.equalsIgnoreCase("form16A")) {
				return Map.of("message", "Invalid type. Must be 'form16' or 'form16A'", "status", "error");
			}

			String basePath = financialYear + "/" + type.toLowerCase() + "/" + tanNumber + "/";

			String originalFileName = file.getOriginalFilename();
			if (originalFileName == null) {
				throw new IllegalArgumentException("File name cannot be null");
			}

			if (originalFileName.toLowerCase().endsWith(".zip")) {
				try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
					ZipEntry entry;
					while ((entry = zis.getNextEntry()) != null) {
						if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".pdf")) {
							String fileName = Paths.get(entry.getName()).getFileName().toString();
							String s3Key = basePath + fileName;

							// ✅ Read the current entry fully
							byte[] fileBytes = zis.readAllBytes(); // Reads only this file's content

							// ✅ Upload to S3
							s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(s3Key)
									.contentType("application/pdf").build(), RequestBody.fromBytes(fileBytes));

							System.out.println("✅ Uploaded: " + s3Key);
						}
						zis.closeEntry();
					}
				}
			} else if (originalFileName.toLowerCase().endsWith(".pdf")) {
				// Handle single PDF file
				String s3Key = basePath + originalFileName;

				s3Client.putObject(
						PutObjectRequest.builder().bucket(bucketName).key(s3Key).contentType("application/pdf").build(),
						RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

				System.out.println("✅ Uploaded: " + s3Key);
			} else {
				return Map.of("message", "Unsupported file type. Only PDF and ZIP are allowed", "status", "error");
			}

			// Dashboard counts
			DashBoardresponse dashBoardresponse = userService.viewDashBoard(null);
			if (type.equalsIgnoreCase("form16")) {
				response.put("form16Count", dashBoardresponse.getForm16Count());
			} else if (type.equalsIgnoreCase("form16a")) {
				response.put("form16ACount", dashBoardresponse.getForm16ACount());
			}

			response.put("message", "File uploaded and processed successfully.");
			response.put("status", "success");

		} catch (Exception e) {
			response.put("message", "Upload failed: " + e.getMessage());
			response.put("status", "error");
			e.printStackTrace();
		}

		return response;
	}

}
