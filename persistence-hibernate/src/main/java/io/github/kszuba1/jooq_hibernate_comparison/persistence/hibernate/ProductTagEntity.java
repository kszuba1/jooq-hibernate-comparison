package io.github.kszuba1.jooq_hibernate_comparison.persistence.hibernate;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_tag")
public class ProductTagEntity {

	@EmbeddedId
	private ProductTagId id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("productId")
	@JoinColumn(name = "product_id")
	private ProductEntity product;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("tagId")
	@JoinColumn(name = "tag_id")
	private TagEntity tag;

	protected ProductTagEntity() {
	}

	ProductTagId getId() {
		return id;
	}

	ProductEntity getProduct() {
		return product;
	}

	TagEntity getTag() {
		return tag;
	}

}
