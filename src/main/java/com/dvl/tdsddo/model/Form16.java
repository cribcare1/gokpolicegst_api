package com.dvl.tdsddo.model;

import java.time.LocalDateTime;

import com.dvl.tdsddo.util.TdsUtil;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "form_16")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Form16 {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne
	@JoinColumn(name = "ddo_id", nullable = false)
	private User ddo; // Reference to DDO (Admin selects this)

	@Column(name = "financial_year", nullable = false)
	private String financialYear;

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "pan_number", nullable = false)
	private String panNumber;

	@Column(name = "mobile_number", nullable = false)
	private String mobileNumber;

	@Column(name = "email", nullable = false)
	private String email;

	@Column(name = "file_path", nullable = false)
	private String filePath; // Store PDF file path

	@Column(name = "created_time", updatable = false)
	private LocalDateTime createdDate;

	@Column(name = "updated_time")
	private LocalDateTime updatedDate;

	@PrePersist
	protected void onCreate() {
		this.createdDate = TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal();
		this.updatedDate = TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal();
	}

	@PostUpdate
	protected void onUpdate() {
		this.updatedDate = TdsUtil.changeCurrentTimeToLocalDateTimeFromGmtToISTLocal();
	}

}
