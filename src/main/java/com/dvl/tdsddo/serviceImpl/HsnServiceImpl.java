package com.dvl.tdsddo.serviceImpl;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.HSNMaster;
import com.dvl.tdsddo.model.HsnGstHistory;
import com.dvl.tdsddo.repository.HSNRepository;
import com.dvl.tdsddo.repository.HsnHistoryRepository;
import com.dvl.tdsddo.request.HSNRequest;
import com.dvl.tdsddo.response.ApiResponse;
import com.dvl.tdsddo.response.HSNMasterDto;
import com.dvl.tdsddo.service.HsnService;
import com.dvl.tdsddo.service.UserService;
import com.dvl.tdsddo.util.TdsUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HsnServiceImpl implements HsnService {
    private final HSNRepository hsnRepository;
    private final UserService userService;
    private final HsnHistoryRepository historyRepository;



    @Override
    @Transactional
    public ApiResponse saveOrUpdateHSN(HSNRequest request) {
        try {
            boolean isNew = (request.getId() == null);

            // Check existing HSN by Code
            HSNMaster existing = hsnRepository.findByHsnCode(request.getHsnCode()).orElse(null);

            // Duplicate validation while Update
            if (!isNew && existing != null && !existing.getId().equals(request.getId())
                    && TdsDdoConstant.ACTIVE.equals(existing.getStatus())) {

                return new ApiResponse(TdsDdoConstant.ERROR,
                        "HSN Code already exists for another active record", null);
            }

            // Reactivate case
            if (isNew && existing != null &&
                    TdsDdoConstant.INACTIVE.equals(existing.getStatus())) {

                HSNMaster inactive = existing;

                HSNMaster old = new HSNMaster();
                BeanUtils.copyProperties(inactive, old);

                BeanUtils.copyProperties(request, inactive);
                inactive.setStatus(TdsDdoConstant.ACTIVE);
                inactive.setUpdateBy(request.getCreatedBy());

                HSNMaster saved = hsnRepository.save(inactive);

                userService.saveAuditLogAsync("hsn_master", saved.getId().toString(),
                        "status", old.getStatus(), saved.getStatus(),
                        "REACTIVATE", request.getCreatedBy());

                return new ApiResponse(TdsDdoConstant.SUCCESS,
                        "Inactive HSN Reactivated Successfully", saved);
            }

            // Create / Update Entity
            HSNMaster hsn = isNew ? new HSNMaster()
                    : hsnRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("HSN not found"));

            // Backup previous values BEFORE update
            HSNMaster oldCopy = null;
            if (!isNew) {
                oldCopy = new HSNMaster();
                BeanUtils.copyProperties(hsn, oldCopy);
            }

            // Update Fields
            BeanUtils.copyProperties(request, hsn);
            if (hsn.getStatus() == null) hsn.setStatus(TdsDdoConstant.ACTIVE);
            hsn.setUpdateBy(request.getCreatedBy());
            HSNMaster saved = hsnRepository.save(hsn);

            // INSERT case
            if (isNew) {
                userService.saveAuditLogAsync("hsn_master", saved.getId().toString(),
                        null, null, null,
                        "INSERT", request.getCreatedBy());
            }
            // UPDATE case with Audit & History
            else {
                logIfChanged("hsnCode", oldCopy.getHsnCode(), saved.getHsnCode(), saved, request.getCreatedBy());
                logIfChanged("serviceName", oldCopy.getServiceName(), saved.getServiceName(), saved, request.getCreatedBy());
                logIfChanged("totalGst", oldCopy.getTotalGst(), saved.getTotalGst(), saved, request.getCreatedBy());
                logIfChanged("igst", oldCopy.getIgst(), saved.getIgst(), saved, request.getCreatedBy());
                logIfChanged("cgst", oldCopy.getCgst(), saved.getCgst(), saved, request.getCreatedBy());
                logIfChanged("sgst", oldCopy.getSgst(), saved.getSgst(), saved, request.getCreatedBy());

                // 🏛 History Insert Only if % Changed
                if (!Objects.equals(oldCopy.getIgst(), saved.getIgst()) ||
                        !Objects.equals(oldCopy.getCgst(), saved.getCgst()) ||
                        !Objects.equals(oldCopy.getSgst(), saved.getSgst()) ||
                        !Objects.equals(oldCopy.getTotalGst(), saved.getTotalGst())) {

                    LocalDate today = TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal().toLocalDate();

                    HsnGstHistory history = HsnGstHistory.builder()
                            .hsnId(saved.getId())
                            .igst(oldCopy.getIgst())
                            .cgst(oldCopy.getCgst())
                            .sgst(oldCopy.getSgst())
                            .totalGst(oldCopy.getTotalGst())
                            .effectiveFrom(oldCopy.getUpdatedDate() != null ?
                                    oldCopy.getUpdatedDate().toLocalDate() : today)
                            .effectiveTo(today)
                            .build();

                    historyRepository.save(history);
                }
            }

            return new ApiResponse(TdsDdoConstant.SUCCESS,
                    isNew ? "HSN Added Successfully" : "HSN Updated Successfully", saved);

        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
        }
    }


//    public ApiResponse saveOrUpdateHSN(HSNRequest request) {
//        try {
//            boolean isNew = false;
//
//            // 🔍 Check if HSN exists regardless of status
//            HSNMaster existingHSN = hsnRepository.findByHsnCode(request.getHsnCode()).orElse(null);
//
//            if (existingHSN != null && TdsDdoConstant.ACTIVE.equals(existingHSN.getStatus())){
//                return new ApiResponse(TdsDdoConstant.SUCCESS,
//                        " HSN code exist.", null);
//            }
//
//            // ⚙️ CASE 1: Reactivate if found INACTIVE
//            if (existingHSN != null && TdsDdoConstant.INACTIVE.equals(existingHSN.getStatus())) {
//                existingHSN.setStatus(TdsDdoConstant.ACTIVE);
//                existingHSN.setServiceName(request.getServiceName());
//                existingHSN.setGstId(request.getGstId());
//                existingHSN.setTotalGst(request.getTotalGst());
//                existingHSN.setIgst(request.getIgst());
//                existingHSN.setCgst(request.getCgst());
//                existingHSN.setSgst(request.getSgst());
//                existingHSN.setUpdateBy(request.getCreatedBy());
//
//                HSNMaster reactivated = hsnRepository.save(existingHSN);
//
//                // 🪵 Log reactivation as UPDATE
//                userService.saveAuditLogAsync("hsn_master",
//                        reactivated.getId().toString(),
//                        "status",
//                        TdsDdoConstant.INACTIVE,
//                        TdsDdoConstant.ACTIVE,
//                        "REACTIVATE",
//                        request.getCreatedBy());
//
//                return new ApiResponse(TdsDdoConstant.SUCCESS,
//                        "Inactive HSN reactivated successfully", reactivated);
//            }
//
//            // ⚙️ CASE 2: Prevent duplicate active record
//            if (existingHSN != null && TdsDdoConstant.ACTIVE.equals(existingHSN.getStatus()) && request.getId() == null) {
//                return new ApiResponse(TdsDdoConstant.ERROR, "HSN Code already exists", null);
//            }
//
//            // ⚙️ CASE 3: Normal create/update
//            HSNMaster hsn = (request.getId() != null)
//                    ? hsnRepository.findById(request.getId())
//                    .orElseThrow(() -> new RuntimeException("HSN not found"))
//                    : new HSNMaster();
//
////
//            // Keep old copy for audit and history comparison
//            HSNMaster oldCopy = new HSNMaster();
//            BeanUtils.copyProperties(hsn, oldCopy);
//
//            if (request.getHsnCode() != null) hsn.setHsnCode(request.getHsnCode());
//            if (request.getServiceName() != null) hsn.setServiceName(request.getServiceName());
//            if (request.getTotalGst() != null) hsn.setTotalGst(request.getTotalGst());
//            if (request.getGstId() != null) hsn.setGstId(request.getGstId());
//            if (request.getIgst() != null) hsn.setIgst(request.getIgst());
//            if (request.getCgst() != null) hsn.setCgst(request.getCgst());
//            if (request.getSgst() != null) hsn.setSgst(request.getSgst());
//            if (hsn.getStatus() == null) hsn.setStatus(TdsDdoConstant.ACTIVE);
//
//            if (request.getCreatedBy() != null) {
//                hsn.setUpdateBy(request.getCreatedBy());
//            }
//
//            if (request.getId() == null) isNew = true;
//
//            HSNMaster saved = hsnRepository.save(hsn);
//
//            // 🪵 Audit logs
//            if (isNew) {
//                userService.saveAuditLogAsync("hsn_master",
//                        saved.getId().toString(), null, null, null,
//                        "INSERT", request.getCreatedBy());
//            } else {
//                logIfChanged("hsnCode", oldCopy.getHsnCode(), hsn.getHsnCode(), saved, request.getCreatedBy());
//                logIfChanged("serviceName", oldCopy.getServiceName(), hsn.getServiceName(), saved, request.getCreatedBy());
//                logIfChanged("iGST", oldCopy.getIgst(), hsn.getIgst(), saved, request.getCreatedBy());
//                logIfChanged("cGST", oldCopy.getCgst(), hsn.getCgst(), saved, request.getCreatedBy());
//                logIfChanged("sGST", oldCopy.getSgst(), hsn.getSgst(), saved, request.getCreatedBy());
//
//                // 🔹 Save history if any GST percentage changed
//                if (!Objects.equals(oldCopy.getIgst(), hsn.getIgst()) ||
//                        !Objects.equals(oldCopy.getCgst(), hsn.getCgst()) ||
//                        !Objects.equals(oldCopy.getSgst(), hsn.getSgst()) ||
//                        !Objects.equals(oldCopy.getTotalGst(), hsn.getTotalGst())) {
//
//                    HsnGstHistory history = HsnGstHistory.builder()
//                            .hsnId(saved.getId())
//                            .igst(oldCopy.getIgst())
//                            .cgst(oldCopy.getCgst())
//                            .sgst(oldCopy.getSgst())
//                            .totalGst(oldCopy.getTotalGst())
//                            .effectiveFrom(oldCopy.getUpdatedDate() != null ? oldCopy.getUpdatedDate().toLocalDate() : TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal().toLocalDate())
//                            .effectiveTo(TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal().toLocalDate())
//                            .build();
//
//                    historyRepository.save(history);
//                }
//            }
//
//            return new ApiResponse(TdsDdoConstant.SUCCESS,
//                    isNew ? "HSN Added Successfully" : "HSN Updated Successfully", saved);
//
//        } catch (Exception e) {
//            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
//        }
//    }


    @Override
    @Transactional
    public ApiResponse deleteHSN(Integer id, Integer updatedBy) {
        try {
            HSNMaster hsn = hsnRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("HSN not found"));

            if (TdsDdoConstant.INACTIVE.equals(hsn.getStatus())) {
                return new ApiResponse(TdsDdoConstant.ERROR, "HSN already inactive", null);
            }

            String oldStatus = hsn.getStatus();
            hsn.setStatus(TdsDdoConstant.INACTIVE);
            hsn.setUpdateBy(updatedBy);

            hsnRepository.save(hsn);

            userService.saveAuditLogAsync("hsn_master", hsn.getId().toString(),
                    "status", oldStatus, TdsDdoConstant.INACTIVE, "DELETE", updatedBy);

            return new ApiResponse(TdsDdoConstant.SUCCESS, "HSN deleted successfully", hsn);

        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
        }
    }

    @Override
    public ApiResponse getAllHSN(Integer gstId) {
        try {
            List<HSNMasterDto> list=new ArrayList<>();
            if(gstId!=null){
                 list = hsnRepository.findAllHSNByGstId(gstId,TdsDdoConstant.ACTIVE);
            }else {
                list = hsnRepository.findAllHSN(TdsDdoConstant.ACTIVE);
            }
            if (list.isEmpty()) {
                return new ApiResponse(TdsDdoConstant.ERROR, "No records found", null);
            }
            return new ApiResponse(TdsDdoConstant.SUCCESS, "HSN list fetched successfully", list);
        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
        }
    }

    @Override
    public ApiResponse getHsnGstHistory(Integer hsnId) {
        try {
            if (hsnId == null) {
                return new ApiResponse(TdsDdoConstant.ERROR, "HSN ID must not be null", null);
            }

            // Fetch history list ordered by effectiveFrom descending
            List<HsnGstHistory> historyList = historyRepository
                    .findByHsnIdOrderByEffectiveFromDesc(hsnId);

            if (historyList.isEmpty()) {
                return new ApiResponse(TdsDdoConstant.SUCCESS, "No GST history found for HSN", null);
            }

            return new ApiResponse(TdsDdoConstant.SUCCESS, "GST history fetched successfully", historyList);

        } catch (Exception e) {
            // Catch all unexpected errors
            return new ApiResponse(TdsDdoConstant.ERROR, "Failed to fetch GST history: " + e.getMessage(), null);
        }
    }


    private void logIfChanged(String column, String oldVal, String newVal,
                              HSNMaster hsn, Integer updatedBy) {
        if (!equalsOrNull(oldVal, newVal)) {
            userService.saveAuditLogAsync("hsn_master", hsn.getId().toString(),
                    column, oldVal, newVal, "UPDATE", updatedBy);
        }
    }

    private boolean equalsOrNull(String oldVal, String newVal) {
        if (oldVal == null && newVal == null) return true;
        if (oldVal == null || newVal == null) return false;
        return oldVal.equals(newVal);
    }
}
