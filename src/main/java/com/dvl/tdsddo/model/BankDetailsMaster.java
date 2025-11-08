package com.dvl.tdsddo.model;

import jakarta.persistence.*;
import lombok.*;
import com.dvl.tdsddo.util.EncryptionUtil;

@Entity
@Table(name = "bank_details_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BankDetailsMaster extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "bank_name", columnDefinition = "TEXT")
    private String bankName;

    @Column(name = "branch_name", columnDefinition = "TEXT")
    private String branchName;

    @Column(name = "account_number", columnDefinition = "TEXT")
    private String accountNumber; // 🔒 Encrypted data stored as TEXT

    @Column(name = "account_type", columnDefinition = "TEXT")
    private String accountType;

    @Column(name = "account_name", columnDefinition = "TEXT")
    private String accountName;

    @Column(name = "ifsc_code", columnDefinition = "TEXT")
    private String ifscCode; // 🔒 Encrypted data stored as TEXT

    @Column(name = "micr_code", columnDefinition = "TEXT")
    private String micrCode;

    @Column(name = "gst_id")
    private Integer gstId;

    @Column(name = "status", columnDefinition = "TEXT")
    private String status;

    // 🔒 Encrypt before saving or updating
    @PrePersist
    @PreUpdate
    private void encryptSensitiveData() {
        EncryptionUtil util = new EncryptionUtil();
        if (accountNumber != null && !accountNumber.startsWith("ENC(")) {
            accountNumber = "ENC(" + util.encrypt(accountNumber) + ")";
        }
        if (ifscCode != null && !ifscCode.startsWith("ENC(")) {
            ifscCode = "ENC(" + util.encrypt(ifscCode) + ")";
        }
    }

    // 🔓 Decrypt after loading
    @PostLoad
    private void decryptSensitiveData() {
        EncryptionUtil util = new EncryptionUtil();
        if (accountNumber != null && accountNumber.startsWith("ENC(")) {
            accountNumber = util.decrypt(accountNumber.substring(4, accountNumber.length() - 1));
        }
        if (ifscCode != null && ifscCode.startsWith("ENC(")) {
            ifscCode = util.decrypt(ifscCode.substring(4, ifscCode.length() - 1));
        }
    }
}
