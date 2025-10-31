package com.dvl.tdsddo.serviceImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.User;
import com.dvl.tdsddo.repository.UserRepository;
import com.dvl.tdsddo.request.AuthRequest;
import com.dvl.tdsddo.request.EditDDORequest;
import com.dvl.tdsddo.response.DDOResponse;
import com.dvl.tdsddo.response.DashBoardresponse;
import com.dvl.tdsddo.response.LoginResponse;
import com.dvl.tdsddo.response.ViewDDOResponse;
import com.dvl.tdsddo.security.JwtService;
import com.dvl.tdsddo.service.UserService;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CommonPrefix;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

@Service
public class UserserviceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder encoder;

	@Autowired
	private S3Client s3Client;

	@Value("${aws.s3.bucket}")
	private String bucketName;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private AuthenticationManager authenticationManager;

	private static final String BASE_UPLOAD_PATH = "C:/uploads/tdsddo/user/financialyear/";

	public Map<String, Object> addAdmin(User user) {
		Map<String, Object> response = new HashMap<>();

		// Check if username already exists
		if (userRepository.existsByUserName(user.getUserName())) {
			return Map.of(TdsDdoConstant.MESSAGE, "", TdsDdoConstant.STATUS, TdsDdoConstant.ERROR);
		}

		// Check if email already exists
		if (userRepository.existsByEmail(user.getEmail())) {
			return Map.of(TdsDdoConstant.MESSAGE, "Email already exists", TdsDdoConstant.STATUS, TdsDdoConstant.ERROR);
		}

		// Check if mobile number already exists
		if (userRepository.existsByMobileNumber(user.getMobileNumber())) {
			return Map.of(TdsDdoConstant.MESSAGE, "Mobile number already exists", TdsDdoConstant.STATUS,
					TdsDdoConstant.ERROR);
		}

		// Save the user
		user.setRole("ADMIN");
		user.setStatus(TdsDdoConstant.ACTIVE); // Default status
		user.setPassword(encoder.encode(user.getPassword())); // Encrypt password

		User savedUser = userRepository.save(user);

		response.put(TdsDdoConstant.MESSAGE, "Admin added successfully");
		response.put(TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS);
		response.put("userId", savedUser.getId());

		return response;
	}

	private Map<String, Object> addUser(User user, String role) {
		Map<String, Object> response = new HashMap<>();

		// Check if username already exists
		if (userRepository.existsByUserName(user.getUserName())) {
			return Map.of(TdsDdoConstant.MESSAGE, "User name alreay exists", TdsDdoConstant.STATUS,
					TdsDdoConstant.ERROR);
		}

		// Check if email already exists
		if (userRepository.existsByEmail(user.getEmail())) {
			return Map.of(TdsDdoConstant.MESSAGE, "Email already exists", TdsDdoConstant.STATUS, TdsDdoConstant.ERROR);
		}

		// Check if mobile number already exists
		if (userRepository.existsByMobileNumber(user.getMobileNumber())) {
			return Map.of(TdsDdoConstant.MESSAGE, "Mobile number already exists", TdsDdoConstant.STATUS,
					TdsDdoConstant.ERROR);
		}

		// Save the user
		user.setRole(role.toUpperCase());
		user.setStatus(TdsDdoConstant.ACTIVE); // Default status
		user.setPassword(encoder.encode(user.getPassword()));

		User savedUser = userRepository.save(user);

		response.put(TdsDdoConstant.MESSAGE, "user added successfully");
		response.put(TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS);
		response.put("userId", savedUser.getId());

		return response;
	}

	// Add DDO under Admin
	@Override
	public Map<String, Object> addDDO(User user, Integer adminId) {
		try {
			User admin = userRepository.findById(adminId).orElse(null);
			if (admin == null) {
				return Map.of(TdsDdoConstant.MESSAGE, "Provide valid admin details.", TdsDdoConstant.STATUS,
						TdsDdoConstant.ERROR);
			}
			user.setAdmin(admin); // Link this DDO to the Admin
			return addUser(user, "DDO");
		} catch (Exception e) {
			return Map.of("message", "Authentication failed: " + e.getMessage(), "status", "error");

		}
	}

//	@Override
//	public Map<String, Object> getAllActiveDDOs(Integer adminId) {
//		List<User> activeDdoUsers = userRepository.findByRoleAndStatusAndAdminId("DDO", "active", adminId);
//		if (activeDdoUsers == null || (activeDdoUsers != null && activeDdoUsers.isEmpty())) {
//			return Map.of(TdsDdoConstant.MESSAGE, "No active DDOs found for the given admin.", TdsDdoConstant.STATUS,
//					TdsDdoConstant.ERROR);
//		}
//
//		List<DDOResponse> ddoResponses = activeDdoUsers.stream().map(user -> DDOResponse.builder().id(user.getId())
//				.fullName(user.getFullName()).tanNumber(user.getDdoTan()).build()).collect(Collectors.toList());
//		return Map.of(TdsDdoConstant.MESSAGE, "DDO list.", TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS,
//				TdsDdoConstant.DDO_LIST, ddoResponses);
//	}
//
//	public String getJwttokenResponse(AuthRequest authRequest) throws Exception {
//		return "You are not a valid user";
//	}

	@Override
	public Map<String, Object> getAllActiveDDOs(Integer adminId, int page, int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());
		Page<User> activeDdoPage = userRepository.findByRoleAndStatusAndAdminId("DDO", "active", adminId, pageable);

		if (activeDdoPage == null || activeDdoPage.isEmpty()) {
			return Map.of(TdsDdoConstant.MESSAGE, "No active DDOs found for the given admin.", TdsDdoConstant.STATUS,
					TdsDdoConstant.ERROR);
		}

		List<DDOResponse> ddoResponses = activeDdoPage.getContent().stream().map(user -> DDOResponse.builder()
				.id(user.getId()).fullName(user.getFullName()).tanNumber(user.getDdoTan()).build())
				.collect(Collectors.toList());

		Map<String, Object> response = new HashMap<>();
		response.put(TdsDdoConstant.MESSAGE, "DDO list.");
		response.put(TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS);
		response.put(TdsDdoConstant.DDO_LIST, ddoResponses);
		response.put("currentPage", activeDdoPage.getNumber());
		response.put("totalPages", activeDdoPage.getTotalPages());
		response.put("totalElements", activeDdoPage.getTotalElements());

		return response;
	}

	@Autowired
	private PasswordEncoder passwordEncoder; // Use the same encoder

	@Override
	public Map<String, Object> loginUsingUserNamePassword(AuthRequest authRequest) {
		if (authRequest == null || authRequest.getUserName() == null || authRequest.getUserName().isEmpty()) {
			return Map.of("message", "User name should be filled.", "status", "error");
		}

		// Fetch user by username
		User user = userRepository.findByUserName(authRequest.getUserName()).orElse(null);
		if (user == null) {
			return Map.of("message", "Invalid username or password.", "status", "error");
		}

		// Check user status
		if ("inactive".equals(user.getStatus())) {
			return Map.of("message", "Your account is inactive.", "status", "error");
		}

		if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
			return Map.of("message", "Invalid username or password.", "status", "error");
		}

		if (user.getRole() != null && !user.getRole().equalsIgnoreCase(authRequest.getRole())) {
			return Map.of("message", "Invalid account type. Please log in using the correct role: Admin or DDO.",
					"status", "error");
		}

		try {
			// Authenticate user credentials
			Authentication auth = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(authRequest.getUserName(), authRequest.getPassword()));

			if (!auth.isAuthenticated()) {
				return Map.of("message", "Invalid username or password.", "status", "error");
			}

			// Generate JWT token
			String token = jwtService.generateToken(authRequest.getUserName());
			LoginResponse loginResponse = null;

			if (user.getRole() != null && user.getRole().equalsIgnoreCase("ddo")) {
				DashBoardresponse dashBoardresponse = viewDashBoard(user.getDdoTan());
				// Build LoginResponse
				loginResponse = new LoginResponse(user.getId(), user.getFullName(), user.getUserName(),
						user.getMobileNumber(), user.getEmail(), user.getRole(), token, 0,
						dashBoardresponse.getForm16ACount(), dashBoardresponse.getForm16Count());
			}

			if (user.getRole() != null && user.getRole().equalsIgnoreCase("admin")) {
				DashBoardresponse dashBoardresponse = viewDashBoard(null);
				List<User> ddoCount = userRepository.findByRoleAndStatus("DDO", "active");

				// Build LoginResponse
				loginResponse = new LoginResponse(user.getId(), user.getFullName(), user.getUserName(),
						user.getMobileNumber(), user.getEmail(), user.getRole(), token, ddoCount.size(),
						dashBoardresponse.getForm16ACount(), dashBoardresponse.getForm16Count());
			}

			return Map.of("message", "You have logged in successfully.", "login_response", loginResponse, "status",
					"success");

		} catch (Exception exception) {
			exception.printStackTrace();
			return Map.of("message", "Authentication failed: " + exception.getMessage(), "status", "error");
		}
	}

	@Override
	public Map<String, Object> createUserWithAdminCheck(User user, Integer adminId) {
		try {
			// Admin check (only if adminId is provided)
			if (adminId != null) {
				User admin = userRepository.findById(adminId).orElse(null);
				if (admin == null) {
					return Map.of(TdsDdoConstant.MESSAGE, "Provide valid admin details.", TdsDdoConstant.STATUS,
							TdsDdoConstant.ERROR);
				}
				user.setAdmin(admin); // Link user to admin
			}

			// Uniqueness checks

			if (userRepository.existsByMobileNumber(user.getMobileNumber())) {
				return Map.of(TdsDdoConstant.MESSAGE, "Mobile number already exists", TdsDdoConstant.STATUS,
						TdsDdoConstant.ERROR);
			}

			// Additional uniqueness checks (if needed)
			if (userRepository.existsByDdoTan(user.getDdoTan())) {
				return Map.of(TdsDdoConstant.MESSAGE, "DDO TAN already exists", TdsDdoConstant.STATUS,
						TdsDdoConstant.ERROR);
			}

			if (userRepository.existsByDdoCode(user.getDdoCode())) {
				return Map.of(TdsDdoConstant.MESSAGE, "DDO Code already exists", TdsDdoConstant.STATUS,
						TdsDdoConstant.ERROR);
			}

			// Final save
			user.setRole("DDO");
			user.setStatus(TdsDdoConstant.ACTIVE);
			user.setPassword(encoder.encode(user.getPassword()));
			user.setUserName(user.getDdoTan());

			User savedUser = userRepository.save(user);

			List<User> ddoCount = userRepository.findByRoleAndStatus("DDO", "active");

			return Map.of(TdsDdoConstant.MESSAGE, "User added successfully", TdsDdoConstant.STATUS,
					TdsDdoConstant.SUCCESS, "userId", savedUser.getId(), "ddoCount", ddoCount.size());

		} catch (Exception e) {
			return Map.of("message", "User creation failed: " + e.getMessage(), "status", "error");
		}
	}

	@Override
	public Map<String, Object> viewDdoDetailsUsingId(Integer ddoId) {
		User user = userRepository.findById(ddoId).orElse(null);
		if (user == null) {
			return Map.of(TdsDdoConstant.STATUS, TdsDdoConstant.ERROR, TdsDdoConstant.MESSAGE,
					"There are no DDO details using this id");
		}
		ViewDDOResponse response = ViewDDOResponse.builder().name(user.getFullName())
				.contactNumber(user.getMobileNumber()).contactPerson(user.getContactPerson()).ddocode(user.getDdoCode())
				.responsiblePerson(user.getResponsiblePerson()).tanNumber(user.getDdoTan())
				.designation(user.getDesignation()).build();

		return Map.of(TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS, TdsDdoConstant.MESSAGE, "DDO details.",
				TdsDdoConstant.DDO_DATA, response);
	}

//	@Override
//	public DashBoardresponse viewDashBoard(String tanNumber) {
//		if (tanNumber == null) {
//			return getForm16And16ACountAcrossYears();
//		}
//
//		return getForm16And16ACountAcrossYears(tanNumber);
//	}
//
//	public DashBoardresponse getForm16And16ACountAcrossYears() {
//		Map<String, Long> formCounts = new HashMap<>();
//		DashBoardresponse dashboaredResponse = new DashBoardresponse();
//
//		try {
//			String basePath = BASE_UPLOAD_PATH;
//
//			for (String formType : List.of("form16", "form16a")) {
//				Path formBasePath = Paths.get(basePath);
//
//				// Search all financial year directories
//				long count = 0;
//				if (Files.exists(formBasePath) && Files.isDirectory(formBasePath)) {
//					System.err.println(basePath);
//
//					try (Stream<Path> yearDirs = Files.list(formBasePath)) {
//						count = yearDirs.filter(Files::isDirectory).map(yearDir -> yearDir.resolve(formType))
//								.filter(Files::exists).filter(Files::isDirectory).flatMap(formDir -> {
//									try {
//										return Files.walk(formDir);
//									} catch (IOException e) {
//										return Stream.empty();
//									}
//								}).filter(path -> Files.isRegularFile(path)
//										&& path.toString().toLowerCase().endsWith(".pdf"))
//								.count();
//					}
//				}
//
//				formCounts.put(formType, count);
//			}
//
//			dashboaredResponse.setForm16ACount(formCounts.getOrDefault("form16a", 0L).intValue());
//			dashboaredResponse.setForm16Count(formCounts.getOrDefault("form16", 0L).intValue());
//
//		} catch (IOException e) {
//			dashboaredResponse.setForm16ACount(0);
//			dashboaredResponse.setForm16Count(0);
//		}
//
//		return dashboaredResponse;
//	}
//
//	public DashBoardresponse getForm16And16ACountAcrossYears(String tanNumber) {
//		Map<String, Long> formCounts = new HashMap<>();
//		DashBoardresponse dashboaredResponse = new DashBoardresponse();
//
//		try {
//			Path basePath = Paths.get(BASE_UPLOAD_PATH);
//
//			for (String formType : List.of("form16", "form16a")) {
//				long count = 0;
//
//				if (Files.exists(basePath) && Files.isDirectory(basePath)) {
//					// Loop through all financial year folders
//					try (Stream<Path> yearDirs = Files.list(basePath)) {
//						System.err.println(basePath);
//						count = yearDirs.filter(Files::isDirectory)
//								.map(yearDir -> yearDir.resolve(formType).resolve(tanNumber)).filter(Files::exists)
//								.filter(Files::isDirectory).flatMap(tanDir -> {
//									try {
//										return Files.walk(tanDir);
//									} catch (IOException e) {
//										return Stream.empty();
//									}
//								}).filter(path -> Files.isRegularFile(path)
//										&& path.toString().toLowerCase().endsWith(".pdf"))
//								.count();
//					}
//				}
//
//				formCounts.put(formType, count);
//			}
//
//			dashboaredResponse.setForm16ACount(formCounts.getOrDefault("form16a", 0L).intValue());
//			dashboaredResponse.setForm16Count(formCounts.getOrDefault("form16", 0L).intValue());
//
//		} catch (IOException e) {
//			dashboaredResponse.setForm16ACount(0);
//			dashboaredResponse.setForm16Count(0);
//		}
//
//		return dashboaredResponse;
//	}

	@Override
	public DashBoardresponse viewDashBoard(String tanNumber) {
		if (tanNumber == null) {
			return getForm16And16ACountAcrossYears();
		}
		return getForm16And16ACountAcrossYears(tanNumber);
	}

	// UsingS3
//	public DashBoardresponse getForm16And16ACountAcrossYears() {
//		Map<String, Long> formCounts = new HashMap<>();
//		DashBoardresponse dashBoardResponse = new DashBoardresponse();
//
//		try {
//			for (String formType : List.of("form16", "form16a")) {
//				long count = 0;
//
//				// List all objects under each formType across all years
//				ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(bucketName).prefix("") // Start
//																												// from
//																												// root
//						.build();
//
//				ListObjectsV2Response listResult = s3Client.listObjectsV2(listRequest);
//
//				count = listResult.contents().stream()
//						.filter(s3Object -> s3Object.key().toLowerCase().contains("/" + formType + "/"))
//						.filter(s3Object -> s3Object.key().toLowerCase().endsWith(".pdf")).count();
//
//				formCounts.put(formType, count);
//			}
//
//			dashBoardResponse.setForm16ACount(formCounts.getOrDefault("form16a", 0L).intValue());
//			dashBoardResponse.setForm16Count(formCounts.getOrDefault("form16", 0L).intValue());
//
//		} catch (Exception e) {
//			dashBoardResponse.setForm16ACount(0);
//			dashBoardResponse.setForm16Count(0);
//		}
//
//		return dashBoardResponse;
//	}

	public DashBoardresponse getForm16And16ACountAcrossYears() {
		Map<String, Long> formCounts = new HashMap<>();
		DashBoardresponse dashBoardResponse = new DashBoardresponse();

		try {
			for (String formType : List.of("form16", "form16a")) {
				long count = 0;
				String continuationToken = null;

				do {
					ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
							.bucket(bucketName)
							.prefix("") // or your prefix if you want to limit scope
							.maxKeys(1000);

					if (continuationToken != null) {
						requestBuilder.continuationToken(continuationToken);
					}

					ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

					count += response.contents().stream()
							.filter(obj -> obj.key().toLowerCase().contains("/" + formType + "/"))
							.filter(obj -> obj.key().toLowerCase().endsWith(".pdf"))
							.count();

					continuationToken = response.nextContinuationToken();

				} while (continuationToken != null);

				formCounts.put(formType, count);
			}

			dashBoardResponse.setForm16ACount(formCounts.getOrDefault("form16a", 0L).intValue());
			dashBoardResponse.setForm16Count(formCounts.getOrDefault("form16", 0L).intValue());

		} catch (Exception e) {
			dashBoardResponse.setForm16ACount(0);
			dashBoardResponse.setForm16Count(0);
			e.printStackTrace();
		}

		return dashBoardResponse;
	}


//	public DashBoardresponse getForm16And16ACountAcrossYears(String tanNumber) {
//		Map<String, Long> formCounts = new HashMap<>();
//		DashBoardresponse dashBoardResponse = new DashBoardresponse();
//
//		try {
//			ListObjectsV2Request yearListRequest = ListObjectsV2Request.builder().bucket(bucketName).delimiter("/")
//					.build();
//
//			ListObjectsV2Response yearListResponse = s3Client.listObjectsV2(yearListRequest);
//
//			List<String> financialYears = yearListResponse.commonPrefixes().stream().map(CommonPrefix::prefix)
//					.collect(Collectors.toList());
//
//			for (String formType : List.of("form16", "form16a")) {
//				long totalCount = 0;
//
//				for (String yearPrefix : financialYears) {
//					// Now for each financial year
//					String prefix = yearPrefix + formType + "/" + tanNumber + "/";
//
//					ListObjectsV2Request listRequest = ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix)
//							.build();
//
//					ListObjectsV2Response listResult = s3Client.listObjectsV2(listRequest);
//
//					long count = listResult.contents().stream()
//							.filter(s3Object -> s3Object.key().toLowerCase().endsWith(".pdf")).count();
//
//					totalCount += count;
//				}
//
//				formCounts.put(formType, totalCount);
//			}
//
//			dashBoardResponse.setForm16ACount(formCounts.getOrDefault("form16a", 0L).intValue());
//			dashBoardResponse.setForm16Count(formCounts.getOrDefault("form16", 0L).intValue());
//
//		} catch (Exception e) {
//			dashBoardResponse.setForm16ACount(0);
//			dashBoardResponse.setForm16Count(0);
//		}
//
//		return dashBoardResponse;
//	}


	public DashBoardresponse getForm16And16ACountAcrossYears(String tanNumber) {
		Map<String, Long> formCounts = new HashMap<>();
		DashBoardresponse dashBoardResponse = new DashBoardresponse();

		try {
			ListObjectsV2Request yearListRequest = ListObjectsV2Request.builder()
					.bucket(bucketName)
					.delimiter("/")
					.build();

			ListObjectsV2Response yearListResponse = s3Client.listObjectsV2(yearListRequest);

			List<String> financialYears = yearListResponse.commonPrefixes().stream()
					.map(CommonPrefix::prefix)
					.collect(Collectors.toList());

			for (String formType : List.of("form16", "form16a")) {
				long totalCount = 0;

				for (String yearPrefix : financialYears) {
					String prefix = yearPrefix + formType + "/" + tanNumber + "/";

					String continuationToken = null;
					do {
						ListObjectsV2Request.Builder listRequestBuilder = ListObjectsV2Request.builder()
								.bucket(bucketName)
								.prefix(prefix)
								.maxKeys(1000);

						if (continuationToken != null) {
							listRequestBuilder.continuationToken(continuationToken);
						}

						ListObjectsV2Response listResult = s3Client.listObjectsV2(listRequestBuilder.build());

						long count = listResult.contents().stream()
								.filter(s3Object -> s3Object.key().toLowerCase().endsWith(".pdf"))
								.count();

						totalCount += count;

						continuationToken = listResult.nextContinuationToken();
					} while (continuationToken != null);
				}

				formCounts.put(formType, totalCount);
			}

			dashBoardResponse.setForm16ACount(formCounts.getOrDefault("form16a", 0L).intValue());
			dashBoardResponse.setForm16Count(formCounts.getOrDefault("form16", 0L).intValue());

		} catch (Exception e) {
			dashBoardResponse.setForm16ACount(0);
			dashBoardResponse.setForm16Count(0);
			e.printStackTrace();
		}

		return dashBoardResponse;
	}


//	public DashBoardresponse getForm16And16ACountAcrossYears() {
//		Map<String, Long> formCounts = new HashMap<>();
//		DashBoardresponse dashBoardResponse = new DashBoardresponse();
//
//		String ftpServer = "ftp.ewingstds.com";
//		int ftpPort = 21;
//		String ftpUser = "ewingstds";
//		String ftpPass = "TDSwings#%26";
//
//		FTPClient ftpClient = new FTPClient();
//
//		try {
//			ftpClient.connect(ftpServer, ftpPort);
//			ftpClient.login(ftpUser, ftpPass);
//			ftpClient.enterLocalPassiveMode();
//
//			FTPFile[] yearDirs = ftpClient.listDirectories("/public_html/uploads");
//
//			for (String formType : List.of("form16", "form16a")) {
//				long totalCount = 0;
//
//				for (FTPFile yearDir : yearDirs) {
//					if (!yearDir.isDirectory())
//						continue;
//
//					String formTypePath = "/public_html/uploads/" + yearDir.getName() + "/" + formType.toLowerCase();
//
//					FTPFile[] tanDirs;
//					try {
//						tanDirs = ftpClient.listDirectories(formTypePath);
//					} catch (IOException e) {
//						continue; // skip if path doesn't exist
//					}
//
//					for (FTPFile tanDir : tanDirs) {
//						if (!tanDir.isDirectory())
//							continue;
//
//						String fullPath = formTypePath + "/" + tanDir.getName();
//
//						FTPFile[] files;
//						try {
//							files = ftpClient.listFiles(fullPath);
//						} catch (IOException e) {
//							continue; // skip if path doesn't exist
//						}
//
//						for (FTPFile file : files) {
//							if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
//								totalCount++;
//							}
//						}
//					}
//				}
//
//				formCounts.put(formType.toLowerCase(), totalCount);
//			}
//
//			dashBoardResponse.setForm16Count(formCounts.getOrDefault("form16", 0L).intValue());
//			dashBoardResponse.setForm16ACount(formCounts.getOrDefault("form16a", 0L).intValue());
//
//		} catch (Exception e) {
//			dashBoardResponse.setForm16Count(0);
//			dashBoardResponse.setForm16ACount(0);
//			e.printStackTrace(); // Optional: log the error
//		} finally {
//			try {
//				if (ftpClient.isConnected()) {
//					ftpClient.logout();
//					ftpClient.disconnect();
//				}
//			} catch (IOException ex) {
//				ex.printStackTrace(); // Optional: log disconnect error
//			}
//		}
//
//		return dashBoardResponse;
//	}
//
//	public DashBoardresponse getForm16And16ACountAcrossYears(String tanNumber) {
//		Map<String, Long> formCounts = new HashMap<>();
//		DashBoardresponse dashBoardResponse = new DashBoardresponse();
//
//		String ftpServer = "ftp.ewingstds.com";
//		int ftpPort = 21;
//		String ftpUser = "ewingstds";
//		String ftpPass = "TDSwings#%26";
//
//		FTPClient ftpClient = new FTPClient();
//
//		try {
//			ftpClient.connect(ftpServer, ftpPort);
//			ftpClient.login(ftpUser, ftpPass);
//			ftpClient.enterLocalPassiveMode();
//
//			FTPFile[] yearDirs = ftpClient.listDirectories("/public_html/uploads");
//
//			for (String formType : List.of("form16", "form16a")) {
//				long totalCount = 0;
//
//				for (FTPFile yearDir : yearDirs) {
//					if (!yearDir.isDirectory())
//						continue;
//
//					String folderPath = "/public_html/uploads/" + yearDir.getName() + "/" + formType.toLowerCase() + "/"
//							+ tanNumber;
//
//					try {
//						FTPFile[] files = ftpClient.listFiles(folderPath);
//						for (FTPFile file : files) {
//							if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
//								totalCount++;
//							}
//						}
//					} catch (IOException e) {
//						// Folder might not exist; ignore and continue
//					}
//				}
//
//				formCounts.put(formType.toLowerCase(), totalCount);
//			}
//
//			dashBoardResponse.setForm16Count(formCounts.getOrDefault("form16", 0L).intValue());
//			dashBoardResponse.setForm16ACount(formCounts.getOrDefault("form16a", 0L).intValue());
//
//		} catch (Exception e) {
//			dashBoardResponse.setForm16Count(0);
//			dashBoardResponse.setForm16ACount(0);
//			e.printStackTrace(); // optional: for debugging
//		} finally {
//			try {
//				if (ftpClient.isConnected()) {
//					ftpClient.logout();
//					ftpClient.disconnect();
//				}
//			} catch (IOException ex) {
//				ex.printStackTrace(); // optional: handle disconnect error
//			}
//		}
//
//		return dashBoardResponse;
//	}

	@Override
	public Map<String, Object> getAllActiveDDOs() {
		List<User> ddoList = userRepository.findByRoleAndStatus("DDO", "active");

		if (ddoList.isEmpty()) {
			return Map.of("status", "error", "message", "No active DDOs found.");
		}

		List<DDOResponse> ddoResponses = ddoList.stream().map(user -> DDOResponse.builder().id(user.getId())
				.fullName(user.getFullName()).tanNumber(user.getDdoTan()).build()).collect(Collectors.toList());

		return Map.of("status", "success", "message", "Active DDO list.", "ddoList", ddoResponses);
	}

	@Override
	public Map<String, Object> editDdoDetails(Integer ddoId, EditDDORequest request) {
		User user = userRepository.findById(ddoId).orElse(null);

		if (user == null) {
			return Map.of(TdsDdoConstant.STATUS, TdsDdoConstant.ERROR, TdsDdoConstant.MESSAGE,
					"No DDO found with this ID.");
		}

		// Check mobile number uniqueness if changed and not null
		if (request.getContactNumber() != null && !request.getContactNumber().equals(user.getMobileNumber())
				&& userRepository.existsByMobileNumber(request.getContactNumber())) {
			return Map.of(TdsDdoConstant.STATUS, TdsDdoConstant.ERROR, TdsDdoConstant.MESSAGE,
					"Mobile number already exists");
		}

		// Check DDO TAN uniqueness if changed and not null
		if (request.getTanNumber() != null && !request.getTanNumber().equals(user.getDdoTan())
				&& userRepository.existsByDdoTan(request.getTanNumber())) {
			return Map.of(TdsDdoConstant.STATUS, TdsDdoConstant.ERROR, TdsDdoConstant.MESSAGE,
					"DDO TAN already exists");
		}

		// Check DDO Code uniqueness if changed and not null
		if (request.getDdocode() != null && !request.getDdocode().equals(user.getDdoCode())
				&& userRepository.existsByDdoCode(request.getDdocode())) {
			return Map.of(TdsDdoConstant.STATUS, TdsDdoConstant.ERROR, TdsDdoConstant.MESSAGE,
					"DDO Code already exists");
		}

		// Update only if request fields are not null
		if (request.getName() != null)
			user.setFullName(request.getName());
		if (request.getContactNumber() != null)
			user.setMobileNumber(request.getContactNumber());
		if (request.getContactPerson() != null)
			user.setContactPerson(request.getContactPerson());
		if (request.getDdocode() != null)
			user.setDdoCode(request.getDdocode());
		if (request.getResponsiblePerson() != null)
			user.setResponsiblePerson(request.getResponsiblePerson());
		if (request.getTanNumber() != null)
			user.setDdoTan(request.getTanNumber());
		if (request.getDesignation() != null)
			user.setDesignation(request.getDesignation());

		userRepository.save(user);

		return Map.of(TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS, TdsDdoConstant.MESSAGE,
				"DDO details updated successfully.");
	}

}
