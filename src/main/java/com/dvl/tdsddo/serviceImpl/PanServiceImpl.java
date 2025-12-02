package com.dvl.tdsddo.serviceImpl;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.PanMaster;
import com.dvl.tdsddo.repository.AuditLogRepository;
import com.dvl.tdsddo.repository.GSTRepository;
import com.dvl.tdsddo.repository.PanMasterRepository;
import com.dvl.tdsddo.request.PanMasterRequest;
import com.dvl.tdsddo.response.ApiResponse;
import com.dvl.tdsddo.service.PanService;
import com.dvl.tdsddo.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PanServiceImpl implements PanService {
    private  final PanMasterRepository panMasterRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserService userService;
    private final GSTRepository gstRepository;
//    @Override
//    @Transactional
//    public ApiResponse saveOrUpdatePan(PanMasterRequest request) {
//        try {
//            PanMaster panMaster;
//
//            // ========== UPDATE CASE ==========
//            if (request.getId() != null) {
//                panMaster = panMasterRepository.findById(request.getId())
//                        .orElseThrow(() -> new RuntimeException("PAN record not found"));
//
//                // Check for duplicate PAN Number (if changed)
//                if (!panMaster.getPanNumber().equals(request.getPanNumber())) {
//                    PanMaster existingPan = panMasterRepository.findByPanNumber(request.getPanNumber());
//                    if (existingPan != null && !existingPan.getId().equals(request.getId())) {
//                        return new ApiResponse(TdsDdoConstant.ERROR, "PAN Number already exists", null);
//                    }
//                }
//
//                // Compare and update only if values changed
//                if (!panMaster.getPanName().equals(request.getPanName())) {
//                    userService.saveAuditLog(
//                            "pan_master",
//                            panMaster.getId().toString(),
//                            "panName",
//                            panMaster.getPanName(),
//                            request.getPanName(),
//                            "UPDATE",
//                            request.getCreatedBy()
//                    );
//                    panMaster.setPanName(request.getPanName());
//                }
//
//                if (!panMaster.getPanNumber().equals(request.getPanNumber())) {
//                    userService.saveAuditLog(
//                            "pan_master",
//                            panMaster.getId().toString(),
//                            "panNumber",
//                            panMaster.getPanNumber(),
//                            request.getPanNumber(),
//                            "UPDATE",
//                            request.getCreatedBy()
//                    );
//                    panMaster.setPanNumber(request.getPanNumber());
//                }
//
//                panMaster.setUpdateBy(request.getCreatedBy());
//             //   panMaster.setUpdatedOn(LocalDateTime.now());
//
//                PanMaster updated = panMasterRepository.save(panMaster);
//                return new ApiResponse(TdsDdoConstant.SUCCESS, "PAN updated successfully", updated);
//            }
//
//            // ========== CREATE CASE ==========
//            else {
//                // Check if PAN Number already exists
//                PanMaster existing = panMasterRepository.findByPanNumber(request.getPanNumber());
//                if (existing != null) {
//                    return new ApiResponse(TdsDdoConstant.ERROR, "PAN Number already exists", null);
//                }
//
//                panMaster = new PanMaster();
//                panMaster.setPanName(request.getPanName());
//                panMaster.setPanNumber(request.getPanNumber());
//                panMaster.setCreatedBy(request.getCreatedBy());
//               // panMaster.setCreatedOn(LocalDateTime.now());
//                panMaster.setStatus(TdsDdoConstant.ACTIVE);
//
//                PanMaster saved = panMasterRepository.save(panMaster);
//
//                userService.saveAuditLog(
//                        "pan_master",
//                        saved.getId().toString(),
//                        null,
//                        null,
//                        null,
//                        "INSERT",
//                        request.getCreatedBy()
//                );
//
//                return new ApiResponse(TdsDdoConstant.SUCCESS, "PAN added successfully", saved);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return new ApiResponse(TdsDdoConstant.ERROR, "Operation failed: " + e.getMessage(), null);
//        }
//    }


    @Override
    @Transactional
    public ApiResponse saveOrUpdatePan(PanMasterRequest request) {
        try {
            PanMaster panMaster;

            // ========== UPDATE CASE ==========
            if (request.getId() != null) {
                panMaster = panMasterRepository.findById(request.getId())
                        .orElseThrow(() -> new RuntimeException("PAN record not found"));

                // === Check for duplicate PAN Number (if changed) ===
                if (request.getPanNumber() != null && !request.getPanNumber().equals(panMaster.getPanNumber())) {
                    PanMaster existingPan = panMasterRepository.findByPanNumber(request.getPanNumber());
                    if (existingPan != null && !existingPan.getId().equals(request.getId())) {
                        return new ApiResponse(TdsDdoConstant.ERROR, "PAN Number already exists", null);
                    }
                }

                // === Check for duplicate Mobile (if changed) ===
                if (request.getMobile() != null && !request.getMobile().equals(panMaster.getMobile())) {
                    PanMaster existingMobile = panMasterRepository.findByMobileAndStatus(request.getMobile(), TdsDdoConstant.ACTIVE);
                    if (existingMobile != null && !existingMobile.getId().equals(request.getId())) {
                        return new ApiResponse(TdsDdoConstant.ERROR, "Mobile number already exists", null);
                    }
                }

                // === Compare and update only if non-null & changed ===
                if (request.getPanName() != null && !request.getPanName().equals(panMaster.getPanName())) {
                    userService.saveAuditLog("pan_master", panMaster.getId().toString(), "panName",
                            panMaster.getPanName(), request.getPanName(), "UPDATE", request.getCreatedBy());
                    panMaster.setPanName(request.getPanName());
                }

                if (request.getPanNumber() != null && !request.getPanNumber().equals(panMaster.getPanNumber())) {
                    userService.saveAuditLog("pan_master", panMaster.getId().toString(), "panNumber",
                            panMaster.getPanNumber(), request.getPanNumber(), "UPDATE", request.getCreatedBy());
                    panMaster.setPanNumber(request.getPanNumber());
                }

                if (request.getEmail() != null && !request.getEmail().equals(panMaster.getEmail())) {
                    userService.saveAuditLog("pan_master", panMaster.getId().toString(), "email",
                            panMaster.getEmail(), request.getEmail(), "UPDATE", request.getCreatedBy());
                    panMaster.setEmail(request.getEmail());
                }

                if (request.getMobile() != null && !request.getMobile().equals(panMaster.getMobile())) {
                    userService.saveAuditLog("pan_master", panMaster.getId().toString(), "mobile",
                            panMaster.getMobile(), request.getMobile(), "UPDATE", request.getCreatedBy());
                    panMaster.setMobile(request.getMobile());
                }

                if (request.getAddress() != null && !request.getAddress().equals(panMaster.getAddress())) {
                    userService.saveAuditLog("pan_master", panMaster.getId().toString(), "address",
                            panMaster.getAddress(), request.getAddress(), "UPDATE", request.getCreatedBy());
                    panMaster.setAddress(request.getAddress());
                }

                if (request.getCity() != null && !request.getCity().equals(panMaster.getCity())) {
                    userService.saveAuditLog("pan_master", panMaster.getId().toString(), "city",
                            panMaster.getCity(), request.getCity(), "UPDATE", request.getCreatedBy());
                    panMaster.setCity(request.getCity());
                }

                if (request.getPinCode() != null && !request.getPinCode().equals(panMaster.getPinCode())) {
                    userService.saveAuditLog("pan_master", panMaster.getId().toString(), "pinCode",
                            panMaster.getPinCode(), request.getPinCode(), "UPDATE", request.getCreatedBy());
                    panMaster.setPinCode(request.getPinCode());
                }
                panMaster.setUpdateBy(request.getCreatedBy());
                PanMaster updated = panMasterRepository.save(panMaster);

                return new ApiResponse(TdsDdoConstant.SUCCESS, "PAN updated successfully", updated);
            }

            // ========== CREATE CASE ==========
            else {
                // === Check if PAN Number already exists ===
                PanMaster existingPan = panMasterRepository.findByPanNumber(request.getPanNumber());
                if (existingPan != null) {
                    return new ApiResponse(TdsDdoConstant.ERROR, "PAN Number already exists", null);
                }

                // === Check if Mobile already exists in active record ===
                if (request.getMobile() != null) {
                    PanMaster existingMobile = panMasterRepository.findByMobileAndStatus(request.getMobile(), TdsDdoConstant.ACTIVE);
                    if (existingMobile != null) {
                        return new ApiResponse(TdsDdoConstant.ERROR, "Mobile number already exists", null);
                    }
                }

                // === Save New Record ===
                panMaster = new PanMaster();
                panMaster.setPanName(request.getPanName());
                panMaster.setPanNumber(request.getPanNumber());
                panMaster.setEmail(request.getEmail());
                panMaster.setMobile(request.getMobile());
                panMaster.setCity(request.getCity());
                panMaster.setPinCode(request.getPinCode());
                panMaster.setAddress(request.getAddress());
                panMaster.setCreatedBy(request.getCreatedBy());
                panMaster.setStatus(TdsDdoConstant.ACTIVE);

                PanMaster saved = panMasterRepository.save(panMaster);

                userService.saveAuditLog("pan_master", saved.getId().toString(),
                        null, null, null, "INSERT", request.getCreatedBy());

                return new ApiResponse(TdsDdoConstant.SUCCESS, "PAN added successfully", saved);
            }

        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, "Operation failed: " + e.getMessage(), null);
        }
    }


    @Override
    @Transactional
    public ApiResponse deletePanDetails(Integer panId, Integer createdBy) {
        try {
            // === Check if record exists ===
            PanMaster panMaster = panMasterRepository.findById(panId)
                    .orElseThrow(() -> new RuntimeException("PAN record not found"));

            // === Soft delete ===
            String oldStatus = panMaster.getStatus();
            panMaster.setStatus(TdsDdoConstant.INACTIVE);
            panMaster.setUpdateBy(panMaster.getCreatedBy());
            panMasterRepository.save(panMaster);

            // === Audit log ===
            userService.saveAuditLog("pan_master", panMaster.getId().toString(), "status",
                    oldStatus, TdsDdoConstant.INACTIVE, "DELETE", createdBy);

            return new ApiResponse(TdsDdoConstant.SUCCESS, "PAN deleted successfully", panMaster);

        } catch (RuntimeException e) {
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, "Unexpected error occurred: " + e.getMessage(), null);
        }
    }

 //   @Override
//    public ApiResponse getAllPanDetails(String status) {
//        try {
//            List<PanMaster> panList;
//
//            // === Fetch based on status ===
//            if (status != null && !status.isEmpty()) {
//                if (status.equalsIgnoreCase(TdsDdoConstant.ALL)) {
//                    panList = panMasterRepository.findAll();
//                } else {
//                    panList = panMasterRepository.findByStatusIgnoreCase(status);
//                }
//            } else {
//                // default: only ACTIVE
//                panList = panMasterRepository.findByStatusIgnoreCase(TdsDdoConstant.ACTIVE);
//            }
//
//            if (panList.isEmpty()) {
//                return new ApiResponse(TdsDdoConstant.ERROR, "No PAN records found", null);
//            }
//
//            return new ApiResponse(TdsDdoConstant.SUCCESS, "List of PAN records fetched successfully", panList);
//
//        } catch (Exception e) {
//            return new ApiResponse(TdsDdoConstant.ERROR, "Error fetching PAN records: " + e.getMessage(), null);
//        }
//    }


    @Override
    public ApiResponse getAllPanDetails(String status) {
        try {

            List<PanMaster> panList;

            // === Fetch PANs based on status ===
            if (status != null && !status.isEmpty()) {
                if (status.equalsIgnoreCase(TdsDdoConstant.ALL)) {
                    panList = panMasterRepository.findAll();
                } else {
                    panList = panMasterRepository.findByStatusIgnoreCase(status);
                }
            } else {
                panList = panMasterRepository.findByStatusIgnoreCase(TdsDdoConstant.ACTIVE);
            }

            if (panList.isEmpty()) {
                return new ApiResponse(TdsDdoConstant.ERROR, "No PAN records found", null);
            }

            // === 1 QUERY: Get all PAN IDs that have ACTIVE GST ===
            List<Integer> panIdsWithActiveGST = gstRepository.findAllPanIdsWithActiveGST();

            // === Set isEditable for each PAN ===
            for (PanMaster pan : panList) {
                boolean hasActiveGst = panIdsWithActiveGST.contains(pan.getId());
                pan.setIsEditable(!hasActiveGst);  // Active GST → not editable
            }

            return new ApiResponse(TdsDdoConstant.SUCCESS, "List of PAN records fetched successfully", panList);

        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, "Error fetching PAN records: " + e.getMessage(), null);
        }
    }

}
