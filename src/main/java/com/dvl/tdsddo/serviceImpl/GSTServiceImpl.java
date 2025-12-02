package com.dvl.tdsddo.serviceImpl;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.GSTMaster;
import com.dvl.tdsddo.model.User;
import com.dvl.tdsddo.repository.DdoGstMappingRepository;
import com.dvl.tdsddo.repository.GSTRepository;
import com.dvl.tdsddo.repository.UserRepository;
import com.dvl.tdsddo.request.GSTMasterRequest;
import com.dvl.tdsddo.response.ApiResponse;
import com.dvl.tdsddo.response.DDOGstResponse;
import com.dvl.tdsddo.response.GSTResponse;
import com.dvl.tdsddo.response.LoginResponse;
import com.dvl.tdsddo.service.GSTService;
import com.dvl.tdsddo.service.UserService;
import com.dvl.tdsddo.util.FileServiceUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GSTServiceImpl implements GSTService {
    private final GSTRepository gstRepository;
    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DdoGstMappingRepository ddoGstMappingRepository;
    private final FileServiceUtil fileServiceUtil;

    @Override
    public ApiResponse saveOrUpdateGSTNew(GSTMasterRequest request) {
        {
            try {
                GSTMaster gstMaster;

                if (request.getGstId() != null) {
                    // === UPDATE EXISTING RECORD ===
                    gstMaster = gstRepository.findById(request.getGstId())
                            .orElseThrow(() -> new RuntimeException("GST record not found"));

                    if (!gstMaster.getGstName().equals(request.getGstName())) {
                        userService.saveAuditLog("gst_master", gstMaster.getId().toString(), "gst_name",
                                gstMaster.getGstName(), request.getGstName(), "UPDATE", request.getCreatedBy());
                        gstMaster.setGstName(request.getGstName());
                    }

                    if (!gstMaster.getGstNumber().equals(request.getGstNumber())) {
                        userService.saveAuditLog("gst_master", gstMaster.getId().toString(), "gst_number",
                                gstMaster.getGstNumber(), request.getGstNumber(), "UPDATE", request.getCreatedBy());
                        gstMaster.setGstNumber(request.getGstNumber());
                    }

//                    if (!gstMaster.getGstAddress().equals(request.getGstAddress())) {
//                        userService.saveAuditLog("gst_master", gstMaster.getId().toString(), "gst_address",
//                                gstMaster.getGstAddress(), request.getGstAddress(), "UPDATE", request.getCreatedBy());
//                        gstMaster.setGstAddress(request.getGstAddress());
//                    }

//                    gstMaster.setGstEmail(request.getGstEmail());
//                    gstMaster.setGstMobile(request.getGstMobile());
                    gstMaster.setUpdateBy(request.getCreatedBy());

                    gstRepository.save(gstMaster);
                    return new ApiResponse(TdsDdoConstant.SUCCESS, "GST details updated successfully", gstMaster);

                } else {
                    // === CREATE NEW RECORD ===
                    GSTMaster existing = gstRepository.findByGstNumber(request.getGstNumber());
                    if (existing != null) {
                        return new ApiResponse(TdsDdoConstant.ERROR, "GST number already exists", null);
                    }

                    gstMaster = new GSTMaster();
                    gstMaster.setGstName(request.getGstName());
                    gstMaster.setGstNumber(request.getGstNumber());
                  //  gstMaster.setGstAddress(request.getGstAddress());
//                    gstMaster.setGstEmail(request.getGstEmail());
//                    gstMaster.setGstMobile(request.getGstMobile());
                    gstMaster.setStatus(TdsDdoConstant.ACTIVE);
                    gstMaster.setUpdateBy(request.getCreatedBy());

                    GSTMaster saved = gstRepository.save(gstMaster);
                    userService.saveAuditLog("gst_master", saved.getId().toString(), null, null, null, "INSERT", request.getCreatedBy());

                    return new ApiResponse(TdsDdoConstant.SUCCESS, "GST added successfully", saved);
                }

            } catch (Exception e) {
                return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
            }
        }
    }

    @Override
    @Transactional
    public ApiResponse deleteGST(Integer gstId, Integer createdBy) {
        try {
            GSTMaster gstMaster = gstRepository.findById(gstId)
                    .orElseThrow(() -> new RuntimeException("GST record not found"));

            String oldStatus = gstMaster.getStatus();
            gstMaster.setStatus(TdsDdoConstant.INACTIVE);
            gstMaster.setUpdateBy(createdBy);
            gstRepository.save(gstMaster);

            userService.saveAuditLog("gst_master", gstMaster.getId().toString(),
                    "status", oldStatus, TdsDdoConstant.INACTIVE, "DELETE", createdBy);

            return new ApiResponse(TdsDdoConstant.SUCCESS, "GST deleted successfully", gstMaster);

        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
        }
    }

    @Override
    public ApiResponse getAllGSTDetails(String status) {
        try {
            List<GSTMaster> list;
            if (status != null && !status.isEmpty()) {
                list = gstRepository.findByStatus(status);
            } else {
                list = gstRepository.findAll();
            }

            return new ApiResponse(TdsDdoConstant.SUCCESS, "GST list fetched successfully", list);

        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
        }
    }


    @Override
    @Transactional
    public ApiResponse saveOrUpdateGST(GSTMasterRequest request, MultipartFile file) {
        try {
            boolean isUpdate = request.getGstId() != null;
            GSTMaster gstMaster;

            // 🔍 Fetch existing GST record by number
            GSTMaster existingGST = gstRepository.findByGstNumber(request.getGstNumber());

            // ===================== 🔹 UPDATE CASE =====================
            if (isUpdate) {
                gstMaster = gstRepository.findById(request.getGstId())
                        .orElseThrow(() -> new RuntimeException("GST record not found with ID: " + request.getGstId()));

                // Prevent duplicate GST numbers (same number on another record)
                if (existingGST != null && !existingGST.getId().equals(request.getGstId())
                        && TdsDdoConstant.ACTIVE.equalsIgnoreCase(existingGST.getStatus())) {
                    return new ApiResponse(TdsDdoConstant.ERROR,
                            "GST number already exists and is active for another record", null);
                }
            }
            // ===================== 🔹 CREATE CASE =====================
            else {
                if (existingGST != null) {
                    if (TdsDdoConstant.ACTIVE.equalsIgnoreCase(existingGST.getStatus())) {
                        return new ApiResponse(TdsDdoConstant.ERROR,
                                "GST number already exists and is active", null);
                    } else {
                        // Reactivate inactive GST
                        gstMaster = existingGST;
                        gstMaster.setStatus(TdsDdoConstant.ACTIVE);
                    }
                } else {
                    gstMaster = new GSTMaster();
                    gstMaster.setStatus(TdsDdoConstant.ACTIVE);
                }
            }

            // ===================== 🔹 SET GST DETAILS =====================
            if (notBlank(request.getGstName())) gstMaster.setGstName(request.getGstName());
            if (notBlank(request.getGstHolderName())) gstMaster.setGstHolderName(request.getGstHolderName());
            if (notBlank(request.getGstNumber())) gstMaster.setGstNumber(request.getGstNumber());
            if (request.getStateCode() != null) gstMaster.setStateCode(request.getStateCode());

            // ===================== 🔹 USER HANDLING =====================
            String username = gstMaster.getGstNumber();
            String rawPassword = generatePasswordFromGST(username);

            User user = null;
            if (gstMaster.getUserId() != null) {
                user = userRepository.findById(gstMaster.getUserId()).orElse(new User());
            }

            // Create new user if missing
            if (user == null || user.getId() == null) {
                user = new User();
                user.setRole("GSTIN");
                user.setStatus(TdsDdoConstant.ACTIVE);
                user.setUserName(username);
                user.setPassword(passwordEncoder.encode(rawPassword));
            }

            // Update user fields only if present
            if (notBlank(request.getGstHolderName())) user.setFullName(request.getGstHolderName());
            if (notBlank(request.getEmail())) user.setEmail(request.getEmail());
            if (notBlank(request.getMobile())) user.setMobileNumber(request.getMobile());
            if (notBlank(request.getAddress())) user.setAddress(request.getAddress());
            if (notBlank(request.getCity())) user.setCity(request.getCity());
            if (notBlank(request.getPinCode())) user.setPinCode(request.getPinCode());
            user.setDesignation("GST Officer");

            // Set creator reference
            if (request.getCreatedBy() != null) {
                user.setCreatedBy(userRepository.findById(request.getCreatedBy()).orElse(null));
            }

            user = userRepository.save(user);
            gstMaster.setUserId(user.getId());

            if(file!=null && !file.isEmpty()){
              String fileName=  fileServiceUtil.uploadFile(file,TdsDdoConstant.GST);
              gstMaster.setGstImage(fileName);
            }

            // ===================== 🔹 SAVE GST MASTER =====================
            gstRepository.save(gstMaster);

            // ===================== 🔹 BUILD RESPONSE =====================
            GSTResponse res = new GSTResponse();
            res.setUserId(user.getId());
            res.setEmail(user.getEmail());
            res.setMobile(user.getMobileNumber());
            res.setAddress(user.getAddress());
            res.setCity(user.getCity());
            res.setPinCode(user.getPinCode());
            res.setStateCode(gstMaster.getStateCode());
            res.setGstId(gstMaster.getId());
            res.setGstName(gstMaster.getGstName());
            res.setGstHolderName(gstMaster.getGstHolderName());
            res.setGstNumber(gstMaster.getGstNumber());
            res.setLogo(gstMaster.getGstImage());

            String message = isUpdate ? "GST Master updated successfully"
                    : (existingGST != null ? "GST Master reactivated successfully" : "GST Master created successfully");

            return new ApiResponse(TdsDdoConstant.SUCCESS, message, res);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("gst_number")) {
                return new ApiResponse(TdsDdoConstant.ERROR,
                        "GST Number already exists. Please use a unique one.", null);
            }
            return new ApiResponse(TdsDdoConstant.ERROR, "Database constraint violation", null);
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse(TdsDdoConstant.ERROR, "Failed to save/update GST details: " + e.getMessage(), null);
        }
    }

//    @Override
//    public ApiResponse saveOrUpdateGST(GSTMasterRequest request) {
//        boolean isUpdate = request.getGstId() != null;
//        GSTMaster gstMaster;
//        User responseUser;
//
//        // 🔍 Check if GST already exists by number
//        GSTMaster existingGST = gstRepository.findByGstNumber(request.getGstNumber());
//
//        if (isUpdate) {
//            gstMaster = gstRepository.findById(request.getGstId())
//                    .orElseThrow(() -> new RuntimeException("GST record not found with ID: " + request.getGstId()));
//        }
//        if(isUpdate && existingGST!=null){
//            if (TdsDdoConstant.ACTIVE.equalsIgnoreCase(existingGST.getStatus())) {
//                return new ApiResponse(TdsDdoConstant.ERROR, "GST number already exists and is active", null);
//            }
//        }
//        else {
//            // 🔹 Creating new GST
//            if (existingGST != null) {
//                if (TdsDdoConstant.ACTIVE.equalsIgnoreCase(existingGST.getStatus())) {
//                    return new ApiResponse(TdsDdoConstant.ERROR, "GST number already exists and is active", null);
//                } else {
//                    // Reactivate if inactive
//                    gstMaster = existingGST;
//                    gstMaster.setStatus(TdsDdoConstant.ACTIVE);
//                }
//            } else {
//                gstMaster = new GSTMaster();
//                gstMaster.setStatus(TdsDdoConstant.ACTIVE);
//            }
//        }
//
//        // ✅ Populate/Update GST details only if present in request
//        if (notBlank(request.getGstName())) gstMaster.setGstName(request.getGstName());
//        if (notBlank(request.getGstHolderName())) gstMaster.setGstHolderName(request.getGstHolderName());
//        if (notBlank(request.getGstNumber())) gstMaster.setGstNumber(request.getGstNumber());
//        if (request.getStateCode() != null) gstMaster.setStateCode(request.getStateCode());
//
//        // 🔐 Username = GST Number, Password = first 7 chars of GST + @1
//        String username = gstMaster.getGstNumber();
//        String rawPassword = generatePasswordFromGST(username);
//
//        // 👤 Handle linked User
//        User user;
//        if (gstMaster.getUserId() != null) {
//            user = userRepository.findById(gstMaster.getUserId()).orElse(new User());
//        } else {
//            user = new User();
//            user.setRole("GSTIN");
//            user.setStatus(TdsDdoConstant.ACTIVE);
//        }
//
//        // ✅ Update only non-null/non-empty fields for User
//        if (notBlank(request.getGstHolderName())) user.setFullName(request.getGstHolderName());
//        if (notBlank(username)) user.setUserName(username);
//        if (!isUpdate) user.setPassword(passwordEncoder.encode(rawPassword)); // only set password for new creation
//        if (notBlank(request.getEmail())) user.setEmail(request.getEmail());
//        if (notBlank(request.getMobile())) user.setMobileNumber(request.getMobile());
//        if (notBlank(request.getAddress())) user.setAddress(request.getAddress());
//        if (notBlank(request.getCity())) user.setCity(request.getCity());
//        if (notBlank(request.getPinCode())) user.setPinCode(request.getPinCode());
//        if (notBlank(user.getDesignation())) user.setDesignation("GST Officer");
//
//        if (request.getCreatedBy() != null) {
//            user.setCreatedBy(userRepository.findById(request.getCreatedBy()).orElse(null));
//        }
//
//        user = userRepository.save(user);
//
//        // ✅ Link User and Save GST
//        gstMaster.setUserId(user.getId());
//        gstRepository.save(gstMaster);
//
//        String message;
//        if (isUpdate) {
//            message = "GST Master updated successfully";
//        } else if (existingGST != null) {
//            message = "GST Master reactivated successfully";
//        } else {
//            message = "GST Master created successfully";
//        }
//
//        GSTResponse res= new GSTResponse();
//        res.setAddress(user.getAddress());
//        res.setUserId(user.getId());
//        res.setMobile(user.getMobileNumber());
//        res.setEmail(user.getEmail());
//        res.setStateCode(gstMaster.getStateCode());
//        res.setGstId(gstMaster.getId());
//        res.setGstName(gstMaster.getGstName());
//        res.setGstNumber(gstMaster.getGstNumber());
//        res.setPinCode(user.getPinCode());
//        res.setGstHolderName(gstMaster.getGstHolderName());
//        res.setCity(user.getCity());
//        return new ApiResponse(TdsDdoConstant.SUCCESS, message, res);
//    }

    /** Helper to check if string is not blank */
    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String generatePasswordFromGST(String gstNumber) {
        if (gstNumber.length() >= 7) {
            return gstNumber.substring(0, 7) + "@1";
        }
        return gstNumber + "@1";
    }


    @Override
    public ApiResponse getAllActiveGstDetails() {
        try {
            // 🔹 Fetch all active GSTs with DDO counts
            List<GSTResponse> gstList = gstRepository.findAllActiveGstWithUserAndDdoCount();

            // 🔹 Handle empty result case
            if (gstList == null || gstList.isEmpty()) {
                return new ApiResponse(
                        TdsDdoConstant.ERROR,
                        "No active GST records found.",
                        null
                );
            }

            // 🔹 Return success response
            return new ApiResponse(
                    TdsDdoConstant.SUCCESS,
                    "Active GST records fetched successfully.",
                    gstList
            );

        } catch (Exception e) {
            // 🔹 Catch unexpected errors
            return new ApiResponse(
                    TdsDdoConstant.ERROR,
                    "Error fetching GST details: " + e.getMessage(),
                    null
            );
        }
    }
    @Override
    public ApiResponse getAllDdosByGstId(Integer gstId) {
        try {
            List<DDOGstResponse> ddos = ddoGstMappingRepository.findAllActiveDdosByGstId(gstId);

            if (ddos.isEmpty()) {
                return new ApiResponse("error", "No DDOs found for the given GST", null);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("count", ddos.size());
            result.put("ddos", ddos);

            return new ApiResponse("success", "DDOs fetched successfully", result);
        } catch (Exception e) {
            return new ApiResponse("error", e.getMessage(), null);
        }
    }
}
