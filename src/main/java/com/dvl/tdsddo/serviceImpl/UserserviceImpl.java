package com.dvl.tdsddo.serviceImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.dvl.tdsddo.model.AuditLog;
import com.dvl.tdsddo.model.DdoGStMapping;
import com.dvl.tdsddo.model.GSTMaster;
import com.dvl.tdsddo.repository.*;
import com.dvl.tdsddo.request.DdoMigrationRequest;
import com.dvl.tdsddo.request.UserRequest;
import com.dvl.tdsddo.response.*;
import jakarta.transaction.Transactional;
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
import com.dvl.tdsddo.request.AuthRequest;
import com.dvl.tdsddo.request.EditDDORequest;
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
    private OtpService otpService;

    @Autowired
    private GSTRepository gstRepository;
    @Autowired
    private DdoGstMappingRepository ddoGstMappingRepository;

	@Autowired
	private S3Client s3Client;

	@Value("${aws.s3.bucket}")
	private String bucketName;

	@Autowired
	private JwtService jwtService;
    @Autowired
    BankDetailsRepository bankDetailsRepository;


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

              DDOGstResponse response=  ddoGstMappingRepository.findActiveGstByDdoId(user.getId()).orElse(null);

				loginResponse = new LoginResponse(user.getId(), user.getFullName(), user.getUserName(),
						user.getMobileNumber(), user.getEmail(), user.getRole(), token, user.getCity(),user.getAddress(),user.getPinCode(),null,null,null,null,user.getDdoCode(), user.getArea(),user.getDdoTan(),user.getTanGstIn(),null);
                if (response!=null){
                    loginResponse.setGstName(response.getGstName());
                    loginResponse.setGstNumber(response.getGstNumber());
                    loginResponse.setGstId(response.getCurrentGstId());
                    if(response.getCurrentGstId()!=null){
                        BankDetailsResponse res=bankDetailsRepository.findActiveBankByGstId(response.getCurrentGstId());
                        loginResponse.setBankDetailsResponse(res);
                    }
                }
			}

            if (user.getRole() != null && user.getRole().equalsIgnoreCase("gstin")) {
//                DashBoardresponse dashBoardresponse = viewDashBoard(user.getDdoTan());
               GSTMaster gm= gstRepository.findByUserIdAndStatus(user.getId(),TdsDdoConstant.ACTIVE);
                // Build LoginResponse
                if(gm!=null){
                    loginResponse = new LoginResponse(user.getId(), user.getFullName(), user.getUserName(),
                            user.getMobileNumber(), user.getEmail(), user.getRole(), token, user.getCity(),user.getAddress(),user.getPinCode(), gm.getGstNumber(), gm.getGstName(),gm.getGstHolderName(),gm.getId(),user.getDdoCode(),user.getArea(),null,null,null);
                }else
                loginResponse = new LoginResponse(user.getId(), user.getFullName(), user.getUserName(),
                        user.getMobileNumber(), user.getEmail(), user.getRole(), token, user.getCity(),user.getAddress(),user.getPinCode(),null,null,null,null,user.getDdoCode(),user.getArea(),null,null,null);
            }

			if (user.getRole() != null && user.getRole().equalsIgnoreCase("admin")) {
//				DashBoardresponse dashBoardresponse = viewDashBoard(null);
//				List<User> ddoCount = userRepository.findByRoleAndStatus("DDO", "active");

				// Build LoginResponse
				loginResponse = new LoginResponse(user.getId(), user.getFullName(), user.getUserName(),
						user.getMobileNumber(), user.getEmail(), user.getRole(), token,user.getCity(),user.getAddress(),user.getPinCode(),null,null,null,null,null,user.getArea(),null,null,null);
			}

			return Map.of("message", "You have logged in successfully.", "login_response", loginResponse, "status",
					"success");

		} catch (Exception exception) {
			exception.printStackTrace();
			return Map.of("message", "Authentication failed: " + exception.getMessage(), "status", "error");
		}
	}

//	@Override
//	public Map<String, Object> createUserWithAdminCheck(User user, Integer adminId,Integer gstId) {
//		try {
//			// Admin check (only if adminId is provided)
//			if (adminId != null) {
//				User admin = userRepository.findById(adminId).orElse(null);
//				if (admin == null || !admin.getRole().equalsIgnoreCase("GSTIN")) {
//					return Map.of(TdsDdoConstant.MESSAGE, "Provide valid GSTIN details.", TdsDdoConstant.STATUS,
//							TdsDdoConstant.ERROR);
//				}
//				user.setCreatedBy(admin); // Link user to admin
//			}
//
//			// Uniqueness checks
//
//			if (userRepository.existsByMobileNumber(user.getMobileNumber())) {
//				return Map.of(TdsDdoConstant.MESSAGE, "Mobile number already exists", TdsDdoConstant.STATUS,
//						TdsDdoConstant.ERROR);
//			}
//
//
//			if (userRepository.existsByDdoCode(user.getDdoCode())) {
//				return Map.of(TdsDdoConstant.MESSAGE, "DDO Code already exists", TdsDdoConstant.STATUS,
//						TdsDdoConstant.ERROR);
//			}
//
//			// Final save
//			user.setRole("DDO");
//			user.setStatus(TdsDdoConstant.ACTIVE);
//            String pass=user.getDdoCode()+"@1";
//			user.setPassword(encoder.encode(pass));
//            user.setCity(user.getCity());
//            user.setPinCode(user.getPinCode());
//			user.setUserName(user.getDdoCode());
//
//			User savedUser = userRepository.save(user);
//
////			List<User> ddoCount = userRepository.findByRoleAndStatus("DDO", "active");
//
//			return Map.of(TdsDdoConstant.MESSAGE, "User added successfully", TdsDdoConstant.STATUS,
//					TdsDdoConstant.SUCCESS, "userId", savedUser.getId());
//
//		} catch (Exception e) {
//			return Map.of("message", "User creation failed: " + e.getMessage(), "status", "error");
//		}
//	}

@Override
@Transactional
public Map<String, Object> createUserWithAdminCheck(UserRequest userRequest) {
    try {
        // ✅ 1️⃣ Create new user object
        User user = new User();
        LoginResponse loginResponse= new LoginResponse();

        // ✅ 2️⃣ Admin (GSTIN) validation
        if (userRequest.getGstInUserId() != null) {
            User admin = userRepository.findById(userRequest.getGstInUserId()).orElse(null);
            if (admin == null || !"GSTIN".equalsIgnoreCase(admin.getRole())) {
                return Map.of(
                        TdsDdoConstant.MESSAGE, "Provide valid GSTIN details.",
                        TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
                );
            }
            user.setCreatedBy(admin);
        }

        // ✅ 3️⃣ Duplicate checks (by mobile and DDO code)
        if (userRepository.existsByMobileNumber(userRequest.getMobile())) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "Mobile number already exists",
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
        }

        if (userRequest.getDdoCode() != null && userRepository.existsByDdoCode(userRequest.getDdoCode())) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "DDO Code already exists",
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
        }


        if (userRequest.getDdoTan() != null && userRepository.existsByDdoTanAndStatus(userRequest.getDdoTan(),TdsDdoConstant.ACTIVE)) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "DDO TAN already exists",
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
        }

        // ✅ 4️⃣ Copy basic fields
        user.setFullName(userRequest.getDdoName());
        user.setUserName(userRequest.getDdoCode());
        user.setMobileNumber(userRequest.getMobile());
        user.setEmail(userRequest.getEmail());
        user.setPoliceStation(userRequest.getPoliceStation());
        user.setDdoCode(userRequest.getDdoCode());
        user.setAddress(userRequest.getAddress());
        user.setPinCode(userRequest.getPinCode());
        user.setCity(userRequest.getCity());
        user.setArea(userRequest.getArea());
        user.setDdoTan(userRequest.getDdoTan());
        user.setTanGstIn(userRequest.getTanGstIn());

        // ✅ 5️⃣ Default values
        user.setRole("DDO");
        user.setStatus(TdsDdoConstant.ACTIVE);

        // Default password = DDO code + "@1"
        String pass = (user.getDdoCode() != null ? user.getDdoCode() : "DDO") + "@1";
        user.setPassword(encoder.encode(pass));

        // Username = DDO code if not provided
        if (user.getUserName() == null && user.getDdoCode() != null) {
            user.setUserName(user.getDdoCode());
        }

        // ✅ 6️⃣ Save user
        User savedUser = userRepository.save(user);
        DDOGstResponse response=  ddoGstMappingRepository.findActiveGstByDdoId(user.getId()).orElse(null);
           loginResponse = new LoginResponse(savedUser.getId(), savedUser.getFullName(), user.getUserName(),
                savedUser.getMobileNumber(), user.getEmail(), user.getRole(), null, user.getCity(),user.getAddress(),user.getPinCode(),null,null,null,null,user.getDdoCode(), user.getArea(),user.getDdoTan(),user.getTanGstIn(),null);
        if (response!=null){
            loginResponse.setGstName(response.getGstName());
            loginResponse.setGstNumber(response.getGstNumber());
            loginResponse.setGstId(response.getCurrentGstId());
            if(response.getCurrentGstId()!=null){
                BankDetailsResponse res=bankDetailsRepository.findActiveBankByGstId(response.getCurrentGstId());
                loginResponse.setBankDetailsResponse(res);
            }
        }
//
        loginResponse.setUserId(savedUser.getId());
        loginResponse.setEmail(savedUser.getEmail());
        loginResponse.setCity(savedUser.getCity());
        loginResponse.setAddress(savedUser.getAddress());
        loginResponse.setDdoCode(savedUser.getDdoCode());
        loginResponse.setFullName(savedUser.getFullName());
        loginResponse.setPinCode(savedUser.getPinCode());
        loginResponse.setArea(savedUser.getArea());
        // ✅ 7️⃣ GST mapping
        if (userRequest.getGstId() != null) {
            // Check if this DDO already mapped
            Optional<DdoGStMapping> existingMapping =
                    ddoGstMappingRepository.findByDdoIdAndStatus(savedUser.getId(), TdsDdoConstant.ACTIVE);

            if (existingMapping.isPresent()) {
                return Map.of(
                        TdsDdoConstant.MESSAGE, "This DDO is already mapped to another GST.",
                        TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
                );
            }

            GSTMaster gst = gstRepository.findById(userRequest.getGstId())
                    .orElseThrow(() -> new RuntimeException("GST record not found"));

            DdoGStMapping mapping = DdoGStMapping.builder()
                    .ddoId(savedUser.getId())
                    .fromGst(userRequest.getGstId())
                    .toGST(userRequest.getGstId())
                    .status(TdsDdoConstant.ACTIVE)
                    .build();

            ddoGstMappingRepository.save(mapping);
        }
        otpService.sendCredentialsEmail(loginResponse.getEmail(),loginResponse.getUserName(),user.getPassword());

        // ✅ 8️⃣ Return success response
        return Map.of(
                TdsDdoConstant.MESSAGE, "DDO user created and mapped successfully",
                TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS,
                TdsDdoConstant.LOGIN_RESPONSE,loginResponse
        );

    } catch (Exception e) {
        return Map.of(
                TdsDdoConstant.MESSAGE, "User creation failed: " + e.getMessage(),
                TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
        );
    }
}



    @Override
    @Transactional
    public Map<String, Object> updateUserWithPartialFields(UserRequest userRequest) {
        try {
            // 1️⃣ Validate ID
            if (userRequest.getId() == null) {
                return Map.of(
                        TdsDdoConstant.MESSAGE, "User ID is required for update",
                        TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
                );
            }

            // 2️⃣ Fetch existing user
            User existingUser = userRepository.findById(userRequest.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 3️⃣ Only DDO role can be updated
            if (!"DDO".equalsIgnoreCase(existingUser.getRole())) {
                return Map.of(
                        TdsDdoConstant.MESSAGE, "Only DDO users can be updated",
                        TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
                );
            }

            // 4️⃣ Update only non-null fields
            if (userRequest.getDdoName() != null)
                existingUser.setFullName(userRequest.getDdoName());

            if (userRequest.getMobile() != null) {
                // Check for duplicate mobile
                if (userRepository.existsByMobileNumberAndIdNot(userRequest.getMobile(), existingUser.getId())) {
                    return Map.of(
                            TdsDdoConstant.MESSAGE, "Mobile number already exists",
                            TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
                    );
                }
                existingUser.setMobileNumber(userRequest.getMobile());
            }


            if (userRequest.getDdoTan() != null) {
                // Check for duplicate mobile
                if (userRepository.existsByDdoTanAndStatusAndIdNot(userRequest.getDdoTan(), TdsDdoConstant.ACTIVE,existingUser.getId())) {
                    return Map.of(
                            TdsDdoConstant.MESSAGE, "Ddo TAN number already exists",
                            TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
                    );
                }
                existingUser.setDdoTan(userRequest.getDdoTan());
            }
            if (userRequest.getEmail() != null)
                existingUser.setEmail(userRequest.getEmail());

            if (userRequest.getPoliceStation() != null)
                existingUser.setPoliceStation(userRequest.getPoliceStation());

            if (userRequest.getDdoCode() != null) {
                // Check for duplicate DDO code
                if (userRepository.existsByDdoCodeAndIdNot(userRequest.getDdoCode(), existingUser.getId())) {
                    return Map.of(
                            TdsDdoConstant.MESSAGE, "DDO Code already exists",
                            TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
                    );
                }
                existingUser.setDdoCode(userRequest.getDdoCode());
            }

            if (userRequest.getAddress() != null)
                existingUser.setAddress(userRequest.getAddress());

            if(userRequest.getTanGstIn()!=null){
                existingUser.setTanGstIn(userRequest.getTanGstIn());
            }
            if (userRequest.getArea() != null)
                existingUser.setArea(userRequest.getArea());

            if (userRequest.getPinCode() != null)
                existingUser.setPinCode(userRequest.getPinCode());

            if (userRequest.getCity() != null)
                existingUser.setCity(userRequest.getCity());

            // 5️⃣ Save updated user
            User updatedUser = userRepository.save(existingUser);

            // 6️⃣ Optional GST remapping
            if (userRequest.getGstId() != null) {
                Optional<DdoGStMapping> existingMapping =
                        ddoGstMappingRepository.findByDdoIdAndStatus(updatedUser.getId(), TdsDdoConstant.ACTIVE);

                if (existingMapping.isPresent()) {
                    DdoGStMapping mapping = existingMapping.get();
                    if (!mapping.getToGST().equals(userRequest.getGstId())) {
                        mapping.setToGST(userRequest.getGstId());
                        ddoGstMappingRepository.save(mapping);
                    }
                } else {
                    DdoGStMapping newMapping = DdoGStMapping.builder()
                            .ddoId(updatedUser.getId())
                            .fromGst(userRequest.getGstId())
                            .toGST(userRequest.getGstId())
                            .status(TdsDdoConstant.ACTIVE)
                            .build();
                    ddoGstMappingRepository.save(newMapping);
                }
            }
            User user=existingUser;

            DDOGstResponse response=  ddoGstMappingRepository.findActiveGstByDdoId(user.getId()).orElse(null);
            LoginResponse   loginResponse = new LoginResponse(user.getId(), user.getFullName(), user.getUserName(),
                    user.getMobileNumber(), user.getEmail(), user.getRole(), null, user.getCity(),user.getAddress(),user.getPinCode(),null,null,null,null,user.getDdoCode(), user.getArea(),user.getDdoTan(),user.getTanGstIn(),null);
            if (response!=null){
                loginResponse.setGstName(response.getGstName());
                loginResponse.setGstNumber(response.getGstNumber());
                loginResponse.setGstId(response.getCurrentGstId());
                if(response.getCurrentGstId()!=null){
                    BankDetailsResponse res=bankDetailsRepository.findActiveBankByGstId(response.getCurrentGstId());
                    loginResponse.setBankDetailsResponse(res);
                }
            }
//            LoginResponse  loginResponse = new LoginResponse(updatedUser.getId(), updatedUser.getFullName(), updatedUser.getUserName(),
//                  updatedUser.getMobileNumber(), updatedUser.getEmail(), updatedUser.getRole(), null, updatedUser.getCity(),updatedUser.getAddress(),updatedUser.getPinCode(),null,null,null,null,updatedUser.getDdoCode(),updatedUser.getArea(),updatedUser.getDdoTan(),updatedUser.getTanGstIn(),null);
            // ✅ 7️⃣ Success Response
            return Map.of(
                    TdsDdoConstant.MESSAGE, "DDO user updated successfully",
                    TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS,
                    TdsDdoConstant.LOGIN_RESPONSE, loginResponse
            );

        } catch (Exception e) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "User update failed: " + e.getMessage(),
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
        }
    }


    @Override
    @Transactional
    public Map<String, Object> deleteDdoById(Integer ddoUserId) {
        try {
            // 1️⃣ Validate user existence
            User existingUser = userRepository.findById(ddoUserId)
                    .orElseThrow(() -> new RuntimeException("DDO user not found"));

            // 2️⃣ Ensure only DDO users can be deleted
            if (!"DDO".equalsIgnoreCase(existingUser.getRole())) {
                return Map.of(
                        TdsDdoConstant.MESSAGE, "Only DDO users can be deleted",
                        TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
                );
            }

            // 3️⃣ Mark user as inactive (soft delete)
            existingUser.setStatus(TdsDdoConstant.INACTIVE);
            userRepository.save(existingUser);

            // 4️⃣ Also deactivate their GST mapping
           DdoGStMapping mappings = ddoGstMappingRepository.findByDdoIdAndStatus(
                    ddoUserId, TdsDdoConstant.ACTIVE
            ).orElse(null);

            if(mappings!=null){
                mappings.setStatus(TdsDdoConstant.INACTIVE);
            ddoGstMappingRepository.save(mappings);

            }


            // ✅ 5️⃣ Return success response
            return Map.of(
                    TdsDdoConstant.MESSAGE, "DDO user deleted successfully",
                    TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS,
                    "userId", ddoUserId
            );

        } catch (Exception e) {
            return Map.of(
                    TdsDdoConstant.MESSAGE, "Failed to delete DDO user: " + e.getMessage(),
                    TdsDdoConstant.STATUS, TdsDdoConstant.ERROR
            );
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
            LoginResponse loginResponse= new LoginResponse();

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

        loginResponse.setUserId(savedUser.getId());
        loginResponse.setUserName(savedUser.getUserName());
        loginResponse.setRole(savedUser.getRole());
        loginResponse.setCity(savedUser.getCity());
        loginResponse.setAddress(savedUser.getAddress());
        loginResponse.setEmail(savedUser.getEmail());
        loginResponse.setMobileNumber(savedUser.getMobileNumber());
        loginResponse.setFullName( savedUser.getFullName());
        loginResponse.setPinCode(savedUser.getPinCode());
        response.put(TdsDdoConstant.MESSAGE, "Admin updated successfully");
        response.put(TdsDdoConstant.STATUS, TdsDdoConstant.SUCCESS);
        response.put(TdsDdoConstant.LOGIN_RESPONSE, loginResponse);

        return response;
    }


    @Override
    @Transactional
    public ApiResponse migrateDdosBetweenGsts(DdoMigrationRequest request) {
        try {
            // ✅ Validate GSTs (2 queries)
            GSTMaster fromGst = gstRepository.findById(request.getFromGstId())
                    .orElseThrow(() -> new RuntimeException("From GST not found"));
            GSTMaster toGst = gstRepository.findById(request.getToGstId())
                    .orElseThrow(() -> new RuntimeException("To GST not found"));

            if (!TdsDdoConstant.ACTIVE.equalsIgnoreCase(fromGst.getStatus()) ||
                    !TdsDdoConstant.ACTIVE.equalsIgnoreCase(toGst.getStatus())) {
                return new ApiResponse(TdsDdoConstant.ERROR, "Both GSTs must be active", null);
            }

            // ✅ Fetch all active mappings for given DDOs in ONE query
            List<DdoGStMapping> existingMappings =
                    ddoGstMappingRepository.findAllByDdoIdInAndStatus(request.getDdoIds(), TdsDdoConstant.ACTIVE);

            Map<Integer, DdoGStMapping> mappingByDdo =
                    existingMappings.stream().collect(Collectors.toMap(DdoGStMapping::getDdoId, m -> m));

            List<DdoGStMapping> newMappings = new ArrayList<>();
            List<Integer> moved = new ArrayList<>();
            List<Integer> failed = new ArrayList<>();

            for (Integer ddoId : request.getDdoIds()) {
                DdoGStMapping existing = mappingByDdo.get(ddoId);

                if (existing == null ||
                        (existing.getToGST() != null && !Objects.equals(existing.getToGST(), request.getFromGstId()))) {
                    failed.add(ddoId);
                    continue;
                }


                // Mark old mapping inactive (in-memory)
                existing.setStatus(TdsDdoConstant.INACTIVE);

                // Prepare new mapping
                DdoGStMapping newMap = DdoGStMapping.builder()
                        .ddoId(ddoId)
                        .fromGst(request.getFromGstId())
                        .toGST(request.getToGstId())
                        .status(TdsDdoConstant.ACTIVE)
                        .build();

                newMappings.add(newMap);
                moved.add(ddoId);
            }

            // ✅ Save all updates & inserts in batch
            ddoGstMappingRepository.saveAll(existingMappings);
            ddoGstMappingRepository.saveAll(newMappings);

            // 🔹 Optional batch audit logging
//            userService.saveBulkAuditLogsAsync(
//                    "ddo_gst_mapping",
//                    moved.stream().map(String::valueOf).toList(),
//                    request.getFromGstId().toString(),
//                    request.getToGstId().toString(),
//                    "DDO_TRANSFER",
//                    request.getUpdatedBy()
//            );

            // ✅ Prepare response
            Map<String, Object> result = new HashMap<>();
            result.put("movedDDOs", moved);
            result.put("failedDDOs", failed);
            result.put("totalMoved", moved.size());

            return new ApiResponse(
                    TdsDdoConstant.SUCCESS,
                    "DDO migration completed. " + moved.size() + " moved, " + failed.size() + " failed.",
                    result
            );

        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
        }
    }



    @Override
    public ApiResponse getDashboardStats(Integer gstId) {
        try {
            DashboardStatsResponse stats=null;
            if(gstId!=null){
                stats=userRepository.getDashboardStatsByGst(gstId);
            }else
             stats = userRepository.getActiveDashboardCounts();

            if (stats == null) {
                return new ApiResponse("error", "No dashboard data found", null);
            }

            return new ApiResponse("success", "Dashboard stats fetched successfully", stats);
        } catch (Exception e) {
            return new ApiResponse("error", "Failed to fetch dashboard stats: " + e.getMessage(), null);
        }
    }
    @Override
    public DDOCurrentGstResponse getCurrentGstOfDdo(Integer ddoId) {
        return userRepository.findCurrentGstByDdoId(ddoId)
                .orElseThrow(() -> new RuntimeException("No active GST found for DDO ID " + ddoId));
    }
}
