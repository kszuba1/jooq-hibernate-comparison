package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "category")
public class CategoryEntity {

	@Id
	private UUID id;

	@Column(name = "parent_id")
	private UUID parentId;

	@Column(nullable = false)
	private String name;

	protected CategoryEntity() {
	}

	UUID getId() {
		return id;
	}

	UUID getParentId() {
		return parentId;
	}

	String getName() {
		return name;
	}

}
