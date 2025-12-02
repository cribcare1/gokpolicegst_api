package com.dvl.tdsddo.model;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.dvl.tdsddo.util.TdsUtil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "full_name")
	private String fullName;//ddoName

	@Column(name = "user_name")
	private String userName;

	@Column(name = "password")
	private String password;

	@Column(name = "mobile_number")
	private String mobileNumber;//Mobile

	@Column(name = "email")
	private String email;

	@Column(name = "police_station")
	private String policeStation;

	@Column(name = "ddo_tan")
	private String ddoTan;

    @Column(name = "tan_gst_in")
    private String tanGstIn;

	@Column(name = "ddo_code")
	private String ddoCode;

	@Column(name = "contact_person")
	private String contactPerson;

	@Column(name = "responsible_person")
	private String responsiblePerson;

    private String address;

	@Column(name = "pin")
	private String pinCode;

    private String area;

    @Column(name = "city")
    private String city;

	@Column(name = "role")
	private String role;

	@Column(name = "status")
	private String status;

	@Column(name = "created_time", updatable = false)
	private LocalDateTime createdDate;

	@Column(name = "updated_time")
	private LocalDateTime updatedDate;

	@Column(name = "designation")
	private String designation;

	@ManyToOne
	@JoinColumn(name = "admin_id") // Foreign key reference to Admin user
	private User createdBy;

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
