package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProductTagId implements Serializable {

	@Column(name = "product_id")
	private UUID productId;

	@Column(name = "tag_id")
	private UUID tagId;

	protected ProductTagId() {
	}

	ProductTagId(UUID productId, UUID tagId) {
		this.productId = productId;
		this.tagId = tagId;
	}

	UUID getProductId() {
		return productId;
	}

	UUID getTagId() {
		return tagId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof ProductTagId other)) {
			return false;
		}
		return Objects.equals(productId, other.productId) && Objects.equals(tagId, other.tagId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(productId, tagId);
	}

}
