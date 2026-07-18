package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer")
public class CustomerEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String email;

	@Column(name = "full_name", nullable = false)
	private String fullName;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	protected CustomerEntity() {
	}

	CustomerEntity(UUID id, String email, String fullName, OffsetDateTime createdAt) {
		this.id = id;
		this.email = email;
		this.fullName = fullName;
		this.createdAt = createdAt;
	}

	UUID getId() {
		return id;
	}

	String getEmail() {
		return email;
	}

	void setEmail(String email) {
		this.email = email;
	}

	String getFullName() {
		return fullName;
	}

	void setFullName(String fullName) {
		this.fullName = fullName;
	}

	OffsetDateTime getCreatedAt() {
		return createdAt;
	}

}
