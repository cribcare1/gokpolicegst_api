package com.dvl.tdsddo.service;

import com.dvl.tdsddo.model.CreditNote;
import com.dvl.tdsddo.response.CreditNoteDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CreditNoteService {
    public CreditNote saveOrUpdateCreditNote(CreditNote request);
    public List<CreditNoteDetails> getCreditNotes(Integer ddoId, Integer gstId);
}
