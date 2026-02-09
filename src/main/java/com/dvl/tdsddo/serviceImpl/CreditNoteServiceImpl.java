package com.dvl.tdsddo.serviceImpl;

import com.dvl.tdsddo.constatnt.TdsDdoConstant;
import com.dvl.tdsddo.model.CreditNote;
import com.dvl.tdsddo.model.GSTMaster;
import com.dvl.tdsddo.model.InvoiceMaster;
import com.dvl.tdsddo.model.User;
import com.dvl.tdsddo.repository.CreditNoteRepository;
import com.dvl.tdsddo.repository.GSTRepository;
import com.dvl.tdsddo.repository.InvoiceMasterRepository;
import com.dvl.tdsddo.repository.UserRepository;
import com.dvl.tdsddo.response.CreditNoteDetails;
import com.dvl.tdsddo.service.CreditNoteService;
import com.dvl.tdsddo.util.TdsUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditNoteServiceImpl implements CreditNoteService {

    private final InvoiceMasterRepository invoiceRepo;
    private final CreditNoteRepository creditNoteRepository;
    private final GSTRepository gstRepo;
    private final UserRepository ddoRepo;
    @Transactional
    @Override
    public CreditNote saveOrUpdateCreditNote(CreditNote request) {

        CreditNote creditNote;

        // 🔹 EDIT
        if (request.getId() != null) {
            creditNote = creditNoteRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Credit Note not found"));
        }
        // 🔹 ADD
        else {
            creditNote = new CreditNote();
            creditNote.setCreditNoteNumber(generateCreditNoteNumber(request.getInvoiceId()));
            creditNote.setCreditNoteDate(
                    request.getCreditNoteDate() != null
                            ? request.getCreditNoteDate()
                            : TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal().toLocalDate()
            );
        }

        creditNote.setEInvoicePreView("pending"); // default value
            creditNote.setIrnStatus("pending"); // default value
        creditNote.setEInvoiceStatus("pending"); // default value

        // 🔹 NULL-SAFE FIELD UPDATES
        if (request.getInvoiceId() != null)
            creditNote.setInvoiceId(request.getInvoiceId());

        if (request.getBaseAmount() != null)
            creditNote.setBaseAmount(request.getBaseAmount());

        if (request.getCreditNoteAmount() != null)
            creditNote.setCreditNoteAmount(request.getCreditNoteAmount());

        if (request.getMismatchAmount() != null)
            creditNote.setMismatchAmount(request.getMismatchAmount());




        if (request.getReason() != null)
            creditNote.setReason(request.getReason());

        return creditNoteRepository.save(creditNote);
    }



    @Override
    public List<CreditNoteDetails> getCreditNotes(Integer ddoId, Integer gstId) {

        if (ddoId == null && gstId == null) {
            throw new IllegalArgumentException("Either DDO ID or GST ID must be provided");
        }

        return creditNoteRepository.findCreditNotesByDdoOrGst(ddoId, gstId);
    }

    public String generateCreditNoteNumber(Integer invoiceId) {

        if (invoiceId == null) {
            throw new RuntimeException("Invoice ID must not be null");
        }

        // 🔹 Load Invoice
        InvoiceMaster invoice = invoiceRepo.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found: " + invoiceId));

        // 🔹 Load GST & DDO
        GSTMaster gst = gstRepo.findById(invoice.getGstId())
                .orElseThrow(() -> new RuntimeException("GST not found: " + invoice.getGstId()));

        User ddo = ddoRepo.findById(invoice.getDdoId())
                .orElseThrow(() -> new RuntimeException("DDO not found: " + invoice.getDdoId()));

        // 🔹 Prepare prefix
        String gstNum = gst.getGstNumber();
        String ddoCode = ddo.getDdoCode();

        String last3 = gstNum.substring(gstNum.length() - 3);
        String last4 = ddoCode.substring(ddoCode.length() - 4);

        // 🔹 Financial Year logic
        LocalDate today = LocalDate.now();
        int year = today.getYear() % 100;
        String fy = (today.getMonthValue() >= 4)
                ? String.format("%02d", year)
                : String.format("%02d", year - 1);

        String prefix = last3 + last4 + "/" + fy + "/CN";

        // 🔹 Fetch last CN by DDO + GST + FY
        List<String> lastCNs = creditNoteRepository
                .findLastCreditNoteNumber(invoice.getDdoId(), invoice.getGstId());

        int next = 1;
        if (!lastCNs.isEmpty()) {
            String lastCN = lastCNs.get(0);
            String seq = lastCN.substring(lastCN.lastIndexOf("CN") + 2);
            next = Integer.parseInt(seq) + 1;
        }

        // 🔹 Final Credit Note Number
        return prefix + String.format("%03d", next);
    }

}
