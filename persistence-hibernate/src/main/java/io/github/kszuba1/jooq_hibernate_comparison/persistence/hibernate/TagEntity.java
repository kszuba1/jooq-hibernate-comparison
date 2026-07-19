package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tag")
public class TagEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	protected TagEntity() {
	}

	UUID getId() {
		return id;
	}

	String getName() {
		return name;
	}

}
