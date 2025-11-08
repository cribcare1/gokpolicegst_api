package com.dvl.tdsddo.serviceImpl;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.HSNMaster;
import com.dvl.tdsddo.repository.HSNRepository;
import com.dvl.tdsddo.request.HSNRequest;
import com.dvl.tdsddo.response.ApiResponse;
import com.dvl.tdsddo.service.HsnService;
import com.dvl.tdsddo.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HsnServiceImpl implements HsnService {
    private final HSNRepository hsnRepository;
    private final UserService userService;
    @Override
    @Transactional
    public ApiResponse saveOrUpdateHSN(HSNRequest request) {
        try {
            boolean isNew = false;

            // 🔍 Check if HSN exists regardless of status
            HSNMaster existingHSN = hsnRepository.findByHsnCode(request.getHsnCode()).orElse(null);

            // ⚙️ CASE 1: Reactivate if found INACTIVE
            if (existingHSN != null && TdsDdoConstant.INACTIVE.equals(existingHSN.getStatus())) {
                existingHSN.setStatus(TdsDdoConstant.ACTIVE);
                existingHSN.setServiceName(request.getServiceName());
                existingHSN.setGstId(request.getGstId());
                existingHSN.setTotalGst(request.getTotalGst());
                existingHSN.setIgst(request.getIgst());
                existingHSN.setCgst(request.getCgst());
                existingHSN.setSgst(request.getSgst());
                existingHSN.setUpdateBy(request.getCreatedBy());

                HSNMaster reactivated = hsnRepository.save(existingHSN);

                // 🪵 Log reactivation as UPDATE
                userService.saveAuditLogAsync("hsn_master",
                        reactivated.getId().toString(),
                        "status",
                        TdsDdoConstant.INACTIVE,
                        TdsDdoConstant.ACTIVE,
                        "REACTIVATE",
                        request.getCreatedBy());

                return new ApiResponse(TdsDdoConstant.SUCCESS,
                        "Inactive HSN reactivated successfully", reactivated);
            }

            // ⚙️ CASE 2: Prevent duplicate active record
            if (existingHSN != null && TdsDdoConstant.ACTIVE.equals(existingHSN.getStatus()) && request.getId() == null) {
                return new ApiResponse(TdsDdoConstant.ERROR, "HSN Code already exists", null);
            }

            // ⚙️ CASE 3: Normal create/update
            HSNMaster hsn = (request.getId() != null)
                    ? hsnRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("HSN not found"))
                    : new HSNMaster();

            HSNMaster oldCopy = new HSNMaster();
            BeanUtils.copyProperties(hsn, oldCopy);

            if (request.getHsnCode() != null) hsn.setHsnCode(request.getHsnCode());
            if (request.getServiceName() != null) hsn.setServiceName(request.getServiceName());
            if (request.getTotalGst() != null) hsn.setTotalGst(request.getTotalGst());
            if (request.getGstId() != null) hsn.setGstId(request.getGstId());
            if (request.getIgst() != null) hsn.setIgst(request.getIgst());
            if (request.getCgst() != null) hsn.setCgst(request.getCgst());
            if (request.getSgst() != null) hsn.setSgst(request.getSgst());
            if (hsn.getStatus() == null) hsn.setStatus(TdsDdoConstant.ACTIVE);

            if (request.getCreatedBy() != null) {
                hsn.setUpdateBy(request.getCreatedBy());
                hsn.setUpdateBy(request.getCreatedBy());
            }

            if (request.getId() == null) isNew = true;

            HSNMaster saved = hsnRepository.save(hsn);

            // 🪵 Audit logs
            if (isNew) {
                userService.saveAuditLogAsync("hsn_master",
                        saved.getId().toString(), null, null, null,
                        "INSERT", request.getCreatedBy());
            } else {
                logIfChanged("hsnCode", oldCopy.getHsnCode(), hsn.getHsnCode(), saved, request.getCreatedBy());
                logIfChanged("serviceName", oldCopy.getServiceName(), hsn.getServiceName(), saved, request.getCreatedBy());
                logIfChanged("iGST", oldCopy.getIgst(), hsn.getIgst(), saved, request.getCreatedBy());
                logIfChanged("cGST", oldCopy.getCgst(), hsn.getCgst(), saved, request.getCreatedBy());
                logIfChanged("sGST", oldCopy.getSgst(), hsn.getSgst(), saved, request.getCreatedBy());
            }

            return new ApiResponse(TdsDdoConstant.SUCCESS,
                    isNew ? "HSN Added Successfully" : "HSN Updated Successfully", saved);

        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
        }
    }

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
    public ApiResponse getAllHSN() {
        try {
            List<HSNMaster> list = hsnRepository.findByStatus(TdsDdoConstant.ACTIVE);
            if (list.isEmpty()) {
                return new ApiResponse(TdsDdoConstant.ERROR, "No records found", null);
            }
            return new ApiResponse(TdsDdoConstant.SUCCESS, "HSN list fetched successfully", list);
        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
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
