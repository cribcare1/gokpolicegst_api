package com.dvl.tdsddo.serviceImpl;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.BankDetailsMaster;
import com.dvl.tdsddo.model.GSTMaster;
import com.dvl.tdsddo.repository.BankDetailsRepository;
import com.dvl.tdsddo.repository.GSTRepository;
import com.dvl.tdsddo.request.BankDetailsRequest;
import com.dvl.tdsddo.response.ApiResponse;
import com.dvl.tdsddo.response.BankDetailsResponse;
import com.dvl.tdsddo.service.BankService;
import com.dvl.tdsddo.service.UserService;
import com.dvl.tdsddo.util.EncryptionUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BankServiceImpl implements BankService {
    private final UserService userService;
    private final BankDetailsRepository bankDetailsRepository;
    private final GSTRepository gstRepository;
//    @Override
//    @Transactional
//    public ApiResponse saveOrUpdateBank(BankDetailsRequest request) {
//        try {
//            EncryptionUtil util = new EncryptionUtil();
//
//            //  Encrypt for duplicate check only
//            String encryptedAcc = "ENC(" + util.encrypt(request.getAccountNumber()) + ")";
//
//            //  Duplicate check (only for new record)
//            if (request.getId() == null &&
//                    bankDetailsRepository.existsByAccountNumberAndStatus(encryptedAcc, TdsDdoConstant.ACTIVE)) {
//                return new ApiResponse(TdsDdoConstant.ERROR, "This Account Number already exists", null);
//            }
//
//            //  Fetch GST details
//            GSTMaster gst = gstRepository.findById(request.getGstId())
//                    .orElseThrow(() -> new RuntimeException("GST record not found"));
//
//            boolean isNew = false;
//            BankDetailsMaster bank;
//
//            if (request.getId() != null) {
//                bank = bankDetailsRepository.findById(request.getId())
//                        .orElseThrow(() -> new RuntimeException("Bank record not found"));
//            } else {
//                bank = new BankDetailsMaster();
//                isNew = true;
//            }
//
//            //  Keep old copy for audit comparison
//            BankDetailsMaster oldBank = new BankDetailsMaster();
//            BeanUtils.copyProperties(bank, oldBank);
//
//            // Partial updates only for provided fields
//            if (request.getBankName() != null) bank.setBankName(request.getBankName());
//            if (request.getBranchName() != null) bank.setBranchName(request.getBranchName());
//            if (request.getAccountNumber() != null) bank.setAccountNumber(request.getAccountNumber()); // entity encrypts
//            if (request.getAccountType() != null) bank.setAccountType(request.getAccountType());
//            if (request.getAccountName() != null) bank.setAccountName(request.getAccountName());
//            if (request.getIfscCode() != null) bank.setIfscCode(request.getIfscCode()); // entity encrypts
//            if (request.getMicrCode() != null) bank.setMicrCode(request.getMicrCode());
//            if (request.getGstId() != null) bank.setGstId(request.getGstId());
//            if (request.getStatus() != null) bank.setStatus(request.getStatus());
//            else if (bank.getStatus() == null) bank.setStatus(TdsDdoConstant.ACTIVE);
//
//            //  Audit user fields
//            if (isNew && request.getCreatedBy() != null)
//                bank.setUpdateBy(request.getCreatedBy());
//            if (request.getCreatedBy() != null)
//                bank.setUpdateBy(request.getCreatedBy());
//
//            //  Save record
//            BankDetailsMaster saved = bankDetailsRepository.save(bank);
//
//            // ASYNC AUDIT LOGGING
//            if (isNew) {
//                userService.saveAuditLogAsync("bank_details_master",
//                        saved.getId().toString(), null, null, null,
//                        "INSERT", request.getCreatedBy());
//            } else {
//                logIfChanged("bankName", oldBank.getBankName(), bank.getBankName(), saved, request.getCreatedBy());
//                logIfChanged("branchName", oldBank.getBranchName(), bank.getBranchName(), saved, request.getCreatedBy());
//                logIfChanged("accountNumber", oldBank.getAccountNumber(), bank.getAccountNumber(), saved, request.getCreatedBy());
//                logIfChanged("accountType", oldBank.getAccountType(), bank.getAccountType(), saved, request.getCreatedBy());
//                logIfChanged("accountName", oldBank.getAccountName(), bank.getAccountName(), saved, request.getCreatedBy());
//                logIfChanged("ifscCode", oldBank.getIfscCode(), bank.getIfscCode(), saved, request.getCreatedBy());
//                logIfChanged("micrCode", oldBank.getMicrCode(), bank.getMicrCode(), saved, request.getCreatedBy());
//                logIfChanged("gstId",
//                        oldBank.getGstId() != null ? oldBank.getGstId().toString() : null,
//                        bank.getGstId() != null ? bank.getGstId().toString() : null,
//                        saved, request.getCreatedBy());
//                logIfChanged("status", oldBank.getStatus(), bank.getStatus(), saved, request.getCreatedBy());
//            }
//
//            return new ApiResponse(
//                    TdsDdoConstant.SUCCESS,
//                    isNew ? "Bank Added Successfully" : "Bank Updated Successfully",
//                    saved
//            );
//
//        } catch (Exception e) {
//            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
//        }
//    }


    @Override
    @Transactional
    public ApiResponse saveOrUpdateBank(BankDetailsRequest request) {
        try {
            boolean isNew = false;
            BankDetailsMaster bank;

            // 🔍 Duplicate check (only for new record)
            if (request.getId() == null &&
                    bankDetailsRepository.existsByAccountNumberAndStatus(request.getAccountNumber(), TdsDdoConstant.ACTIVE)) {
                return new ApiResponse(TdsDdoConstant.ERROR, "This Account Number already exists", null);
            }

            // 🔍 Fetch GST details
            GSTMaster gst = gstRepository.findById(request.getGstId())
                    .orElseThrow(() -> new RuntimeException("GST record not found"));

            // 🏦 Determine if new or update
            if (request.getId() != null) {
                bank = bankDetailsRepository.findById(request.getId())
                        .orElseThrow(() -> new RuntimeException("Bank record not found"));
            } else {
                bank = new BankDetailsMaster();
                isNew = true;
            }

            // 🔁 Keep old copy for audit comparison
            BankDetailsMaster oldBank = new BankDetailsMaster();
            BeanUtils.copyProperties(bank, oldBank);

            // ✏️ Partial updates only for non-null fields
            if (request.getBankName() != null) bank.setBankName(request.getBankName());
            if (request.getBranchName() != null) bank.setBranchName(request.getBranchName());
            if (request.getAccountNumber() != null) bank.setAccountNumber(request.getAccountNumber());
            if (request.getAccountType() != null) bank.setAccountType(request.getAccountType());
            if (request.getAccountName() != null) bank.setAccountName(request.getAccountName());
            if (request.getIfscCode() != null) bank.setIfscCode(request.getIfscCode());
            if (request.getMicrCode() != null) bank.setMicrCode(request.getMicrCode());
            if (request.getGstId() != null) bank.setGstId(request.getGstId());
            if (request.getStatus() != null) bank.setStatus(request.getStatus());
            else if (bank.getStatus() == null) bank.setStatus(TdsDdoConstant.ACTIVE);

            // 👤 Audit fields
            if (isNew && request.getCreatedBy() != null)
                bank.setUpdateBy(request.getCreatedBy());
            if (request.getCreatedBy() != null)
                bank.setUpdateBy(request.getCreatedBy());

            // 💾 Save record
            BankDetailsMaster saved = bankDetailsRepository.save(bank);

            // 🧾 Async Audit Logging
            if (isNew) {
                userService.saveAuditLogAsync(
                        "bank_details_master",
                        saved.getId().toString(),
                        null,
                        null,
                        null,
                        "INSERT",
                        request.getCreatedBy()
                );
            } else {
                logIfChanged("bankName", oldBank.getBankName(), bank.getBankName(), saved, request.getCreatedBy());
                logIfChanged("branchName", oldBank.getBranchName(), bank.getBranchName(), saved, request.getCreatedBy());
                logIfChanged("accountNumber", oldBank.getAccountNumber(), bank.getAccountNumber(), saved, request.getCreatedBy());
                logIfChanged("accountType", oldBank.getAccountType(), bank.getAccountType(), saved, request.getCreatedBy());
                logIfChanged("accountName", oldBank.getAccountName(), bank.getAccountName(), saved, request.getCreatedBy());
                logIfChanged("ifscCode", oldBank.getIfscCode(), bank.getIfscCode(), saved, request.getCreatedBy());
                logIfChanged("micrCode", oldBank.getMicrCode(), bank.getMicrCode(), saved, request.getCreatedBy());
                logIfChanged("gstId",
                        oldBank.getGstId() != null ? oldBank.getGstId().toString() : null,
                        bank.getGstId() != null ? bank.getGstId().toString() : null,
                        saved, request.getCreatedBy());
                logIfChanged("status", oldBank.getStatus(), bank.getStatus(), saved, request.getCreatedBy());
            }

            return new ApiResponse(
                    TdsDdoConstant.SUCCESS,
                    isNew ? "Bank Added Successfully" : "Bank Updated Successfully",
                    saved
            );

        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
        }
    }



    @Override
    @Transactional
    public ApiResponse saveOrUpdateBankNew(BankDetailsRequest request) {
        try {
            GSTMaster gst=null;
            if (request!=null && request.getDdoId()==null){
                gst   = gstRepository.findById(request.getGstId())
                        .orElseThrow(() -> new RuntimeException("GST record not found"));

            }

            // 🔍 Validate GST

            // 🔍 If NEW record → check duplicate
            if (request.getId() == null &&
                    bankDetailsRepository.existsByAccountNumberAndStatus(request.getAccountNumber(), TdsDdoConstant.ACTIVE)) {

                return new ApiResponse(TdsDdoConstant.ERROR,
                        "Bank Account Number already exists", null);
            }

            boolean isNewInsert = (request.getId() == null);

            // ==============================================
            // 🟡 CASE 1: ADD NEW BANK
            // ==============================================
            if (isNewInsert) {

                BankDetailsMaster newBank = new BankDetailsMaster();
                updateFields(newBank, request);   // Fill fields
                newBank.setStatus(TdsDdoConstant.ACTIVE);
                if(request.getDdoId()!=null){
                   newBank.setDdoId(request.getDdoId());
                }else{
                    newBank.setGstId(request.getGstId());
                }

                BankDetailsMaster saved = bankDetailsRepository.save(newBank);

                // AUDIT
                userService.saveAuditLogAsync(
                        "bank_details_master",
                        saved.getId().toString(),
                        null, null, null,
                        "INSERT",
                        request.getCreatedBy()
                );

                return new ApiResponse(
                        TdsDdoConstant.SUCCESS,
                        "Bank Added Successfully",
                        saved
                );
            }

            // ==============================================
            // 🟢 CASE 2: EDIT – Deactivate Old + Insert New
            // ==============================================
            BankDetailsMaster oldBank = bankDetailsRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Bank record not found"));

            // ❌ Deactivate OLD record
            oldBank.setStatus(TdsDdoConstant.INACTIVE);
            bankDetailsRepository.save(oldBank);

            // 🟢 Create NEW record with updated values
            BankDetailsMaster newBank = new BankDetailsMaster();
            copyOldValues(newBank,oldBank);
            updateFields(newBank, request);

            newBank.setId(null);                     // force new insert
            newBank.setStatus(TdsDdoConstant.ACTIVE);
            newBank.setGstId(oldBank.getGstId());    // keep same GST
            newBank.setDdoId(oldBank.getDdoId());    //Keep same DDOId

            BankDetailsMaster savedNew = bankDetailsRepository.save(newBank);

            // AUDIT (difference between oldBank and newBank)
            logIfChanged("bankName", oldBank.getBankName(), newBank.getBankName(), savedNew, request.getCreatedBy());
            logIfChanged("branchName", oldBank.getBranchName(), newBank.getBranchName(), savedNew, request.getCreatedBy());
            logIfChanged("accountNumber", oldBank.getAccountNumber(), newBank.getAccountNumber(), savedNew, request.getCreatedBy());
            logIfChanged("accountType", oldBank.getAccountType(), newBank.getAccountType(), savedNew, request.getCreatedBy());
            logIfChanged("accountName", oldBank.getAccountName(), newBank.getAccountName(), savedNew, request.getCreatedBy());
            logIfChanged("ifscCode", oldBank.getIfscCode(), newBank.getIfscCode(), savedNew, request.getCreatedBy());
            logIfChanged("micrCode", oldBank.getMicrCode(), newBank.getMicrCode(), savedNew, request.getCreatedBy());

            // You are not changing GST in edit, so no GST audit required.

            return new ApiResponse(
                    TdsDdoConstant.SUCCESS,
                    "Bank Updated Successfully",
                    savedNew
            );

        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
        }
    }
    private void updateFields(BankDetailsMaster bank, BankDetailsRequest request) {
        if (request.getBankName() != null) bank.setBankName(request.getBankName());
        if (request.getBranchName() != null) bank.setBranchName(request.getBranchName());
        if (request.getAccountNumber() != null) bank.setAccountNumber(request.getAccountNumber());
        if (request.getAccountType() != null) bank.setAccountType(request.getAccountType());
        if (request.getAccountName() != null) bank.setAccountName(request.getAccountName());
        if (request.getIfscCode() != null) bank.setIfscCode(request.getIfscCode());
        if (request.getMicrCode() != null) bank.setMicrCode(request.getMicrCode());
        if (request.getGstId() != null) bank.setGstId(request.getGstId());
        if (request.getDdoId() != null) bank.setDdoId(request.getDdoId());

    }

    private void copyOldValues(BankDetailsMaster target, BankDetailsMaster source) {
        target.setBankName(source.getBankName());
        target.setBranchName(source.getBranchName());
        target.setAccountNumber(source.getAccountNumber());
        target.setAccountType(source.getAccountType());
        target.setAccountName(source.getAccountName());
        target.setIfscCode(source.getIfscCode());
        target.setMicrCode(source.getMicrCode());
        target.setGstId(source.getGstId());
        target.setDdoId(source.getDdoId());
    }


    @Override
    @Transactional
    public ApiResponse deleteBank(Integer bankId, Integer updatedBy) {
        try {
            //  Fetch record from DB
            BankDetailsMaster bank = bankDetailsRepository.findById(bankId)
                    .orElseThrow(() -> new RuntimeException("Bank record not found"));

            //  Check if already inactive
            if (TdsDdoConstant.INACTIVE.equals(bank.getStatus())) {
                return new ApiResponse(TdsDdoConstant.ERROR, "Bank record already inactive", null);
            }

            // Store old status for audit log
            String oldStatus = bank.getStatus();

            //  Perform soft delete
            bank.setStatus(TdsDdoConstant.INACTIVE);
            bank.setUpdateBy(updatedBy);
            bankDetailsRepository.save(bank);

            //  Log the deletion asynchronously
            userService.saveAuditLogAsync(
                    "bank_details_master",
                    bank.getId().toString(),
                    "status",
                    oldStatus,
                    TdsDdoConstant.INACTIVE,
                    "DELETE",
                    updatedBy
            );

            //  Return success
            return new ApiResponse(TdsDdoConstant.SUCCESS, "Bank deleted successfully", bank);

        } catch (Exception e) {
            //  Error handling
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
        }
    }

    @Override
    public ApiResponse getAllActiveBankDetails(Integer gstId,Integer ddoId) {
        try {
            List<BankDetailsResponse> result=null;
            if(ddoId!=null){
                result =bankDetailsRepository.findAllActiveBanksForDDO(ddoId);

            }
            else{
                result = bankDetailsRepository.findAllActiveBanks(gstId);
            }

            if (result.isEmpty()) {
                return new ApiResponse(TdsDdoConstant.ERROR, "No records found", null);
            }
            return new ApiResponse(TdsDdoConstant.SUCCESS, "Bank details fetched successfully", result);
        } catch (Exception e) {
            return new ApiResponse(TdsDdoConstant.ERROR, e.getMessage(), null);
        }
    }



    // ✅ Helper: Log only when values differ
    private void logIfChanged(String column, String oldVal, String newVal,
                              BankDetailsMaster bank, Integer updatedBy) {
        if (!equalsOrNull(oldVal, newVal)) {
            userService.saveAuditLogAsync("bank_details_master",
                    bank.getId().toString(),
                    column,
                    oldVal,
                    newVal,
                    "UPDATE",
                    updatedBy);
        }
    }

    // ✅ Utility: null-safe compare
    private boolean equalsOrNull(String oldVal, String newVal) {
        if (oldVal == null && newVal == null) return true;
        if (oldVal == null || newVal == null) return false;
        return oldVal.equals(newVal);
    }
}
