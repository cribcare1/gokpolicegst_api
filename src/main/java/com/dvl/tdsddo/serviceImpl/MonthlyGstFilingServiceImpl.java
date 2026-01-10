package com.dvl.tdsddo.serviceImpl;

import com.dvl.tdsddo.Exception.BusinessException;
import com.dvl.tdsddo.Exception.ResourceNotFoundException;
import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.MonthlyGstFiling;
import com.dvl.tdsddo.repository.MonthlyGstFilingRepository;
import com.dvl.tdsddo.request.MonthlyGstFilingRequest;
import com.dvl.tdsddo.response.MonthlyGstFilingResponse;
import com.dvl.tdsddo.service.MonthlyGstFilingService;
import com.dvl.tdsddo.util.FileServiceUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonthlyGstFilingServiceImpl implements MonthlyGstFilingService {

    private final MonthlyGstFilingRepository monthlyGstFilingRepository;
    private final FileServiceUtil fileService;

//    @Override
//    public MonthlyGstFilingResponse saveOrUpdate(MonthlyGstFilingRequest request) {
//
//        MonthlyGstFiling entity;
//
//        if (request.getId() != null) {
//            entity = monthlyGstFilingRepository.findById(request.getId())
//                    .orElseThrow(() -> new ResourceNotFoundException("Data Not Found"));
//        } else {
//            entity = new MonthlyGstFiling();
//        }
//
//        entity.setFilingMonth(request.getFilingMonth());
//        entity.setArnNo(request.getArnNo());
//        entity.setArnDate(request.getArnDate());
//        entity.setDeclaredAmount(request.getDeclaredAmount());
//        entity.setPaidAmount(request.getPaidAmount());
//        entity.setPenaltyAmount(request.getPenaltyAmount());
//        entity.setAckDocument(request.getAckDocument());
//
//        // Calculate Difference
//        BigDecimal diff = request.getDeclaredAmount()
//                .subtract(request.getPaidAmount());
//
//        entity.setDifferenceAmount(diff);
//
////        // Auto-generate Remarks
////        if (diff.compareTo(BigDecimal.ZERO) == 0) {
////            entity.setRemarks("Filing Completed");
////        } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
////            entity.setRemarks("Amount Mismatch");
////        } else {
////            entity.setRemarks("Over Payment");
////        }
//
//        entity.setRemarks(request.getRemark());
//
//        monthlyGstFilingRepository.save(entity);
//        return convertToResponse(entity);
//    }


//@Override
//public MonthlyGstFilingResponse saveOrUpdate(MonthlyGstFilingRequest request, MultipartFile file) throws IOException {
//
//    MonthlyGstFiling entity;
//
//    if (request.getId() != null) {
//        entity = monthlyGstFilingRepository.findById(request.getId())
//                .orElseThrow(() -> new ResourceNotFoundException("Data Not Found"));
//    } else {
//        entity = new MonthlyGstFiling();
//    }
//
//    // Update only if not null
//    if (request.getFilingMonth() != null)
//        entity.setFilingMonth(request.getFilingMonth());
//
//    if (request.getArnNo() != null)
//        entity.setArnNo(request.getArnNo());
//
//    if (request.getArnDate() != null)
//        entity.setArnDate(request.getArnDate());
//
//    if (request.getDeclaredAmount() != null)
//        entity.setDeclaredAmount(request.getDeclaredAmount());
//
//    if (request.getPaidAmount() != null)
//        entity.setPaidAmount(request.getPaidAmount());
//
//    if (request.getPenaltyAmount() != null)
//        entity.setPenaltyAmount(request.getPenaltyAmount());
//
//    if (request.getDdoId() != null)
//        entity.setDdoId(request.getDdoId());
//
//    if (file!=null && !file.isEmpty()) {
//        String fileName = fileService.uploadFile(file, TdsDdoConstant.GST);
//        entity.setAckDocument(fileName);
//    }
//
//    // Recalculate difference only if declared or paid changed
//    if (request.getDeclaredAmount() != null || request.getPaidAmount() != null) {
//
//        BigDecimal declared = entity.getDeclaredAmount() == null ? BigDecimal.ZERO : entity.getDeclaredAmount();
//        BigDecimal paid = entity.getPaidAmount() == null ? BigDecimal.ZERO : entity.getPaidAmount();
//
//        BigDecimal diff = declared.subtract(paid);
//        entity.setDifferenceAmount(diff);
//    }
//
//    // Update remarks only if provided
//    if (request.getRemark() != null) {
//        entity.setRemarks(request.getRemark());
//    }
//
//    monthlyGstFilingRepository.save(entity);
//    return convertToResponse(entity);
//}


@Override
@Transactional
public MonthlyGstFilingResponse saveOrUpdate(MonthlyGstFilingRequest request,
                                             MultipartFile file) throws IOException {

    MonthlyGstFiling entity;

    boolean isUpdate = request.getId() != null;

    if (isUpdate) {
        entity = monthlyGstFilingRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));
    } else {
        entity = new MonthlyGstFiling();
    }

    // -------------------------------
    // 1️⃣  VALIDATE UNIQUE CONSTRAINTS
    // -------------------------------

    // 🔹 Validate DDO + Filing Month uniqueness
    if (request.getDdoId() != null && request.getFilingMonth() != null) {
        boolean exists = monthlyGstFilingRepository
                .existsByDdoIdAndFilingMonthAndIdNot(
                        request.getDdoId(),
                        request.getFilingMonth(),
                        request.getId() == null ? -1 : request.getId()
                );

        if (exists) {
            throw new BusinessException("Filing already exists for this DDO and month");
        }
    }

    // 🔹 Validate ARN uniqueness (when provided)
    if (request.getArnNo() != null && !request.getArnNo().isBlank()) {
        boolean arnExists = monthlyGstFilingRepository
                .existsByArnNoAndIdNot(
                        request.getArnNo(),
                        request.getId() == null ? -1 : request.getId()
                );

        if (arnExists) {
            throw new BusinessException("ARN already exists in another filing");
        }
    }

    // -------------------------------
    // 2️⃣  APPLY FIELD UPDATES (PATCH MODE)
    // -------------------------------
    if (request.getDdoId() != null) entity.setDdoId(request.getDdoId());
    if (request.getFilingMonth() != null) entity.setFilingMonth(request.getFilingMonth());
    if (request.getArnNo() != null) entity.setArnNo(request.getArnNo());
    if (request.getArnDate() != null) entity.setArnDate(request.getArnDate());
    if (request.getDeclaredAmount() != null) entity.setDeclaredAmount(request.getDeclaredAmount());
    if (request.getPaidAmount() != null) entity.setPaidAmount(request.getPaidAmount());
    if (request.getPenaltyAmount() != null) entity.setPenaltyAmount(request.getPenaltyAmount());
    if (request.getFinancialYear() != null) entity.setFinancialYear(request.getFinancialYear());

    // -------------------------------
    // 3️⃣  FILE UPLOAD (ACK DOCUMENT)
    // -------------------------------
    if (file != null && !file.isEmpty()) {
        String fileName = fileService.uploadFile(file, TdsDdoConstant.GST);
        entity.setAckDocument(fileName);
    }

    // -------------------------------
    // 4️⃣  RECALCULATE DIFFERENCE
    // -------------------------------
    BigDecimal declared = entity.getDeclaredAmount() == null
            ? BigDecimal.ZERO : entity.getDeclaredAmount();

    BigDecimal paid = entity.getPaidAmount() == null
            ? BigDecimal.ZERO : entity.getPaidAmount();

    entity.setDifferenceAmount(declared.subtract(paid));

    // -------------------------------
    // 5️⃣  REMARKS
    // -------------------------------
    if (request.getRemark() != null) {
        entity.setRemarks(request.getRemark());
    }

    // -------------------------------
    // 6️⃣  SAVE & RETURN
    // -------------------------------
    entity = monthlyGstFilingRepository.save(entity);

    return convertToResponse(entity);
}


    @Override
    public MonthlyGstFilingResponse getById(Integer id) {
        MonthlyGstFiling entity = monthlyGstFilingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record Not Found"));

        return convertToResponse(entity);
    }
    @Override
    public List<MonthlyGstFilingResponse> getByDdoId(Integer ddoId) {
        List<MonthlyGstFiling> entities = monthlyGstFilingRepository.findByDdoId(ddoId);
        if (entities == null || entities.isEmpty()) {
            throw new ResourceNotFoundException("No records found for ddoId: " + ddoId);
        }
        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MonthlyGstFilingResponse> getByGSTId(Integer gstId) {
        List<MonthlyGstFiling> entities = monthlyGstFilingRepository.findFilingsByGstId(gstId);
        if (entities == null || entities.isEmpty()) {
            throw new ResourceNotFoundException("No records found for ddoId: " + gstId);
        }
        return entities.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }


    @Override
    public List<MonthlyGstFilingResponse> getAll() {
        return monthlyGstFilingRepository.getAllFilings()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private MonthlyGstFilingResponse convertToResponse(MonthlyGstFiling entity) {
        MonthlyGstFilingResponse res = new MonthlyGstFilingResponse();
        res.setId(entity.getId());
        res.setDdoId(entity.getDdoId());
        res.setAckDocument(entity.getAckDocument());
        res.setFilingMonth(entity.getFilingMonth());
        res.setArnNo(entity.getArnNo());
        res.setArnDate(entity.getArnDate());
        res.setDeclaredAmount(entity.getDeclaredAmount());
        res.setPaidAmount(entity.getPaidAmount());
        res.setPenaltyAmount(entity.getPenaltyAmount());
        res.setDifferenceAmount(entity.getDifferenceAmount());
        res.setRemarks(entity.getRemarks());
        res.setFinancialYear(entity.getFinancialYear());
        return res;
    }

    @Override
    public void delete(Integer id) {

        MonthlyGstFiling entity = monthlyGstFilingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Record Not Found"));

        monthlyGstFilingRepository.delete(entity);
    }
}

