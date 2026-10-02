package com.crm.model;

import java.math.BigDecimal;

/** Quote history intentionally has no cost-price field. */
public record QuoteItem(long id, long productId, String code, String name, String unit, BigDecimal quantity,
                        BigDecimal unitPrice, BigDecimal listPrice, BigDecimal floorPrice) { }
