package com.dvl.tdsddo.serviceImpl;

import com.dvl.tdsddo.entity.QuarterlyIncomeTaxFiling;
import com.dvl.tdsddo.repository.QuarterlyIncomeTaxFilingRepository;
import com.dvl.tdsddo.request.QuarterlyIncomeTaxFilingRequest;
import com.dvl.tdsddo.response.QuarterlyIncomeTaxFilingResponse;
import com.dvl.tdsddo.service.QuarterlyIncomeTaxFilingService;
import com.dvl.tdsddo.util.FileServiceUtil;
import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class QuarterlyIncomeTaxFilingServiceImpl implements QuarterlyIncomeTaxFilingService {

    private final QuarterlyIncomeTaxFilingRepository repository;
    private final FileServiceUtil fileServiceUtil;

//    @Override
//    public QuarterlyIncomeTaxFilingResponse saveOrUpdate(QuarterlyIncomeTaxFilingRequest request, MultipartFile file) {
//        QuarterlyIncomeTaxFiling entity;
//        if (request.getId() != null) {
//            entity = repository.findById(request.getId())
//                    .orElseThrow(() -> new RuntimeException("Data Not Found"));
//        } else {
//            entity = new QuarterlyIncomeTaxFiling();
//        }
//
//        // File upload if provided
//        if (file != null && !file.isEmpty()) {
//            try {
//                String uploadedPath = fileServiceUtil.uploadFile(file, TdsDdoConstant.GST);
//                entity.setAckDocument(uploadedPath);
//            } catch (IOException e) {
//                throw new RuntimeException("Failed to upload file", e);
//            }
//        }
//
//        // Partial updates
//        if (request.getDdoId() != null) entity.setDdoId(request.getDdoId());
//
//        if (request.getFiscalYear() != null) entity.setFiscalYear(request.getFiscalYear());
//        if (request.getReturnType() != null) entity.setReturnType(request.getReturnType());
//        if (request.getQuarter() != null) entity.setQuarter(request.getQuarter());
//
//        if (request.getDateOfFiling() != null) entity.setDateOfFiling(request.getDateOfFiling());
//        if (request.getProvisionalReceiptNo() != null) entity.setProvisionalReceiptNo(request.getProvisionalReceiptNo());
//
//        if (request.getDeducteeCount() != null) entity.setDeducteeCount(request.getDeducteeCount());
//        if (request.getTotalChallanAmount() != null) entity.setTotalChallanAmount(request.getTotalChallanAmount());
//        if (request.getTotalTaxDeducted() != null) entity.setTotalTaxDeducted(request.getTotalTaxDeducted());
//
//        if (request.getAnyRevisionFiled() != null) entity.setAnyRevisionFiled(request.getAnyRevisionFiled());
//        // prefer explicit ackDocument in request over uploaded path
//        if (request.getAckDocument() != null) entity.setAckDocument(request.getAckDocument());
//
//        if (request.getRemarks() != null) entity.setRemarks(request.getRemarks());
//
//        // Recalculate difference if either challan or tax changed
//        if (request.getTotalChallanAmount() != null || request.getTotalTaxDeducted() != null) {
//            BigDecimal challan = entity.getTotalChallanAmount() == null ? BigDecimal.ZERO : entity.getTotalChallanAmount();
//            BigDecimal tax = entity.getTotalTaxDeducted() == null ? BigDecimal.ZERO : entity.getTotalTaxDeducted();
//            entity.setDifferenceInReporting(challan.subtract(tax));
//        }
//
//        QuarterlyIncomeTaxFiling saved = repository.save(entity);
//        return convertToResponse(saved);
//    }


    @Override
    @Transactional
    public QuarterlyIncomeTaxFilingResponse saveOrUpdate(
            QuarterlyIncomeTaxFilingRequest request,
            MultipartFile file
    ) {

        QuarterlyIncomeTaxFiling entity;

        if (request.getId() != null) {
            entity = repository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Data Not Found"));
        } else {
            entity = new QuarterlyIncomeTaxFiling();
        }

        // 🔹 Validate uniqueness BEFORE save
        if (request.getDdoId() != null &&
                request.getFiscalYear() != null &&
                request.getReturnType() != null &&
                request.getQuarter() != null) {

            boolean exists;

            if (request.getId() == null) {
                // INSERT case
                exists = repository.existsByDdoIdAndFiscalYearAndReturnTypeAndQuarter(
                        request.getDdoId(),
                        request.getFiscalYear(),
                        request.getReturnType(),
                        request.getQuarter()
                );
            } else {
                // UPDATE case → allow same record but not others
                exists = repository.existsByDdoIdAndFiscalYearAndReturnTypeAndQuarterAndIdNot(
                        request.getDdoId(),
                        request.getFiscalYear(),
                        request.getReturnType(),
                        request.getQuarter(),
                        request.getId()
                );
            }

            if (exists) {
                throw new RuntimeException(
                        "Record already exists for DDO, Fiscal Year, Return Type and Quarter"
                );
            }
        }

        // 🔹 File upload
        if (file != null && !file.isEmpty()) {
            try {
                String uploadedPath = fileServiceUtil.uploadFile(file, TdsDdoConstant.GST);
                entity.setAckDocument(uploadedPath);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload file", e);
            }
        }

        // 🔹 Partial update mapping
        if (request.getDdoId() != null) entity.setDdoId(request.getDdoId());
        if (request.getFiscalYear() != null) entity.setFiscalYear(request.getFiscalYear());
        if (request.getReturnType() != null) entity.setReturnType(request.getReturnType());
        if (request.getQuarter() != null) entity.setQuarter(request.getQuarter());
        if (request.getDateOfFiling() != null) entity.setDateOfFiling(request.getDateOfFiling());
        if (request.getProvisionalReceiptNo() != null) entity.setProvisionalReceiptNo(request.getProvisionalReceiptNo());
        if (request.getDeducteeCount() != null) entity.setDeducteeCount(request.getDeducteeCount());
        if (request.getTotalChallanAmount() != null) entity.setTotalChallanAmount(request.getTotalChallanAmount());
        if (request.getTotalTaxDeducted() != null) entity.setTotalTaxDeducted(request.getTotalTaxDeducted());
        if (request.getAnyRevisionFiled() != null) entity.setAnyRevisionFiled(request.getAnyRevisionFiled());
        if (request.getAckDocument() != null) entity.setAckDocument(request.getAckDocument());
        if (request.getRemarks() != null) entity.setRemarks(request.getRemarks());

        // 🔹 Recalculate difference
        BigDecimal challan = entity.getTotalChallanAmount() == null ? BigDecimal.ZERO : entity.getTotalChallanAmount();
        BigDecimal tax = entity.getTotalTaxDeducted() == null ? BigDecimal.ZERO : entity.getTotalTaxDeducted();
        entity.setDifferenceInReporting(challan.subtract(tax));

        QuarterlyIncomeTaxFiling saved = repository.save(entity);
        return convertToResponse(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public QuarterlyIncomeTaxFilingResponse getById(Long id) {
        QuarterlyIncomeTaxFiling e = repository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        return convertToResponse(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuarterlyIncomeTaxFilingResponse> getAll() {
        return repository.findAll().stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuarterlyIncomeTaxFilingResponse> getByDdoId(Integer ddoId) {
        return repository.findByDdoId(ddoId).stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuarterlyIncomeTaxFilingResponse> getByGstId(Integer gstId) {
        return repository.findByGstId(gstId).stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    private QuarterlyIncomeTaxFilingResponse convertToResponse(QuarterlyIncomeTaxFiling e) {
        QuarterlyIncomeTaxFilingResponse r = new QuarterlyIncomeTaxFilingResponse();
        r.setId(e.getId());
        r.setDdoId(e.getDdoId());
        r.setDdoCode(e.getDdoCode());
        r.setDdoOfficeName(e.getDdoOfficeName());
        r.setTan(e.getTan());
        r.setFiscalYear(e.getFiscalYear());
        r.setReturnType(e.getReturnType());
        r.setQuarter(e.getQuarter());
        r.setDateOfFiling(e.getDateOfFiling());
        r.setProvisionalReceiptNo(e.getProvisionalReceiptNo());
        r.setDeducteeCount(e.getDeducteeCount());
        r.setTotalChallanAmount(e.getTotalChallanAmount());
        r.setTotalTaxDeducted(e.getTotalTaxDeducted());
        r.setAnyRevisionFiled(e.getAnyRevisionFiled());
        r.setAckDocument(e.getAckDocument());
        r.setDifferenceInReporting(e.getDifferenceInReporting());
        r.setRemarks(e.getRemarks());
        r.setCreatedAt(e.getCreatedAt());
        r.setUpdatedAt(e.getUpdatedAt());
        return r;
    }
}
