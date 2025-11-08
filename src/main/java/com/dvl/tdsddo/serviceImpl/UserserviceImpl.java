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

import com.dvl.tdsddo.model.AuditLog;
import com.dvl.tdsddo.model.GSTMaster;
import com.dvl.tdsddo.repository.AuditLogRepository;
import com.dvl.tdsddo.repository.GSTRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
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
@RequiredArgsConstructor
public class UserserviceImpl implements UserService {
    private final AuditLogRepository auditLogRepository;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder encoder;

    @Autowired
    private GSTRepository gstRepository;

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
			user.setCreatedBy(admin); // Link this DDO to the Admin
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
		Page<User> activeDdoPage = userRepository.findByRoleAndStatusAndCreatedBy("DDO", "active", adminId, pageable);

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
				//DashBoardresponse dashBoardresponse = viewDashBoard(user.getDdoTan());
				// Build LoginResponse
				loginResponse = new LoginResponse(user.getId(), user.getFullName(), user.getUserName(),
						user.getMobileNumber(), user.getEmail(), user.getRole(), token, user.getCity(),user.getAddress(),user.getPinCode(),null,null,null,user.getDdoCode());
			}

            if (user.getRole() != null && user.getRole().equalsIgnoreCase("gstin")) {
//                DashBoardresponse dashBoardresponse = viewDashBoard(user.getDdoTan());
               GSTMaster gm= gstRepository.findByUserIdAndStatus(user.getId(),TdsDdoConstant.ACTIVE);
                // Build LoginResponse
                if(gm!=null){
                    loginResponse = new LoginResponse(user.getId(), user.getFullName(), user.getUserName(),
                            user.getMobileNumber(), user.getEmail(), user.getRole(), token, user.getCity(),user.getAddress(),user.getPinCode(), gm.getGstNumber(), gm.getGstName(),gm.getGstHolderName(),user.getDdoCode());
                }else
                loginResponse = new LoginResponse(user.getId(), user.getFullName(), user.getUserName(),
                        user.getMobileNumber(), user.getEmail(), user.getRole(), token, user.getCity(),user.getAddress(),user.getPinCode(),null,null,null,user.getDdoCode());
            }

			if (user.getRole() != null && user.getRole().equalsIgnoreCase("admin")) {
//				DashBoardresponse dashBoardresponse = viewDashBoard(null);
//				List<User> ddoCount = userRepository.findByRoleAndStatus("DDO", "active");

				// Build LoginResponse
				loginResponse = new LoginResponse(user.getId(), user.getFullName(), user.getUserName(),
						user.getMobileNumber(), user.getEmail(), user.getRole(), token,user.getCity(),user.getAddress(),user.getPinCode(),null,null,null,null);
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
				if (admin == null || !admin.getRole().equalsIgnoreCase("GSTIN")) {
					return Map.of(TdsDdoConstant.MESSAGE, "Provide valid GSTIN details.", TdsDdoConstant.STATUS,
							TdsDdoConstant.ERROR);
				}
				user.setCreatedBy(admin); // Link user to admin
			}

			// Uniqueness checks

			if (userRepository.existsByMobileNumber(user.getMobileNumber())) {
				return Map.of(TdsDdoConstant.MESSAGE, "Mobile number already exists", TdsDdoConstant.STATUS,
						TdsDdoConstant.ERROR);
			}


			if (userRepository.existsByDdoCode(user.getDdoCode())) {
				return Map.of(TdsDdoConstant.MESSAGE, "DDO Code already exists", TdsDdoConstant.STATUS,
						TdsDdoConstant.ERROR);
			}

			// Final save
			user.setRole("DDO");
			user.setStatus(TdsDdoConstant.ACTIVE);
            String pass=user.getDdoCode()+"@1";
			user.setPassword(encoder.encode(pass));
            user.setCity(user.getCity());
            user.setPinCode(user.getPinCode());
			user.setUserName(user.getDdoCode());

			User savedUser = userRepository.save(user);

//			List<User> ddoCount = userRepository.findByRoleAndStatus("DDO", "active");

			return Map.of(TdsDdoConstant.MESSAGE, "User added successfully", TdsDdoConstant.STATUS,
					TdsDdoConstant.SUCCESS, "userId", savedUser.getId());

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

    @Async
    @Override
    public void saveAuditLog(String tableName, String recordId, String columnName,
                             String oldValue, String newValue,
                             String actionType, Integer updatedBy) {
        AuditLog log = new AuditLog();
        log.setTableName(tableName);
        log.setRecordId(recordId);
        log.setColumnName(columnName);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setActionType(actionType);
        log.setUpdateBy(updatedBy);
        auditLogRepository.save(log);
    }

    @Async
    public void saveAuditLogAsync(String tableName, String recordId, String columnName,
                                  String oldValue, String newValue,
                                  String actionType, Integer updatedBy) {
        try {
            AuditLog log = new AuditLog();
            log.setTableName(tableName);
            log.setRecordId(recordId);
            log.setColumnName(columnName);
            log.setOldValue(oldValue);
            log.setNewValue(newValue);
            log.setActionType(actionType);
            log.setUpdateBy(updatedBy);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // prevent audit failure from breaking flow
            System.err.println("Audit Log Error: " + e.getMessage());
        }
    }


    @Override
    public Map<String, Object> editAdmin(User updatedUser) {
        Map<String, Object> response = new HashMap<>();

        // ✅ Validate input
        if (updatedUser.getId() == null) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "User ID is required for update",
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
        }

        // ✅ Find existing admin
        User existingUser = userRepository.findById(updatedUser.getId())
                .orElse(null);

        if (existingUser == null) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "Admin not found with ID: " + updatedUser.getId(),
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
        }

        // ✅ Check duplicate validations only if the field is changing
        if (updatedUser.getUserName() != null
                && !updatedUser.getUserName().equals(existingUser.getUserName())
                && userRepository.existsByUserName(updatedUser.getUserName())) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "Username already exists",
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
        }

        if (updatedUser.getEmail() != null
                && !updatedUser.getEmail().equals(existingUser.getEmail())
                && userRepository.existsByEmail(updatedUser.getEmail())) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "Email already exists",
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
        }

        if (updatedUser.getMobileNumber() != null
                && !updatedUser.getMobileNumber().equals(existingUser.getMobileNumber())
                && userRepository.existsByMobileNumber(updatedUser.getMobileNumber())) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "Mobile number already exists",
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
        }

        // ✅ Update only non-null fields
        if (updatedUser.getFullName() != null)
            existingUser.setFullName(updatedUser.getFullName());

        if (updatedUser.getUserName() != null)
            existingUser.setUserName(updatedUser.getUserName());

        if (updatedUser.getPassword() != null)
            existingUser.setPassword(encoder.encode(updatedUser.getPassword()));

        if (updatedUser.getEmail() != null)
            existingUser.setEmail(updatedUser.getEmail());

        if (updatedUser.getMobileNumber() != null)
            existingUser.setMobileNumber(updatedUser.getMobileNumber());

        if (updatedUser.getAddress() != null)
            existingUser.setAddress(updatedUser.getAddress());

        if (updatedUser.getCity() != null)
            existingUser.setCity(updatedUser.getCity());

        if (updatedUser.getPinCode() != null)
            existingUser.setPinCode(updatedUser.getPinCode());

        if (updatedUser.getPoliceStation() != null)
            existingUser.setPoliceStation(updatedUser.getPoliceStation());

        if (updatedUser.getDdoCode() != null)
            existingUser.setDdoCode(updatedUser.getDdoCode());

        if (updatedUser.getDdoTan() != null)
            existingUser.setDdoTan(updatedUser.getDdoTan());


        if (updatedUser.getDesignation() != null)
            existingUser.setDesignation(updatedUser.getDesignation());

        if (updatedUser.getStatus() != null)
            existingUser.setStatus(updatedUser.getStatus());

        // ✅ Save updates
        User savedUser = userRepository.save(existingUser);

        response.put(TdsDdoConstant.MESSAGE, "Admin updated successfully");
        response.put(TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS);
        response.put("userId", savedUser.getId());

        return response;
    }

}
