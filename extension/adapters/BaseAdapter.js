// BaseAdapter - parent class of every store adapter (SDS 3.4).
//
// Each supported store gets one subclass (DarazAdapter, DemoStoreAdapter) that overrides the "get" methods
// for that store's page layout. content.js holds the adapter in a BaseAdapter-typed variable and only calls
// readObservation(), so it never needs to know which store it is on (polymorphism). A new store = one new class.
//
// Every getter returns null when it is not sure. readObservation() then refuses to build an observation
// instead of sending a guess (SDS 3.4: "Could not read this page").

class BaseAdapter {
  constructor(doc, loc) {
    this.document = doc;
    this.location = loc;
  }

  // Does this adapter handle the page at this location? Subclasses override.
  static matches(loc) {
    return false;
  }

  // ---- methods each store overrides (SDS 3.4) ----
  isProductPage() { return false; }
  getSku() { return null; }
  getTitle() { return null; }
  getCurrentPrice() { return null; }        // a string with a plain number, e.g. "5999"
  getClaimedOriginalPrice() { return null; } // the crossed-out "was" price, or null
  hasDiscountBadge() { return false; }
  getImageUrl() { return null; }
  getPriceElement() { return null; }         // where the verdict badge is drawn (next to the price)

  // ---- shared logic: the same for every store ----

  // Only the page address without "?..." and "#...": tracking parameters are never sent (NFR-03).
  getCleanUrl() {
    return this.location.origin + this.location.pathname;
  }

  // Template method: asks the store-specific getters and checks the answers.
  // Returns { ok: true, data } or { ok: false, missing: [field names] }.
  readObservation() {
    const sku = this.getSku();
    const title = this.getTitle();
    const price = this.getCurrentPrice();
    const missing = [];
    if (!sku) { missing.push('sku'); }
    if (!title) { missing.push('title'); }
    if (!price) { missing.push('price'); }
    if (missing.length > 0) {
      return { ok: false, missing: missing };
    }
    return {
      ok: true,
      data: {
        url: this.getCleanUrl(),
        sku: sku,
        title: title.slice(0, 500),
        imageUrl: this.getImageUrl(),
        price: price,
        claimedOriginalPrice: this.getClaimedOriginalPrice(),
        discountBadgeShown: this.hasDiscountBadge() || this.getClaimedOriginalPrice() !== null
      }
    };
  }

  // ---- helpers for subclasses ----

  // "Rs. 12,649" -> "12649", "₨ 5,999.50" -> "5999.50", "abc" -> null. Never guesses.
  static parsePrice(text) {
    if (text === null || text === undefined) {
      return null;
    }
    const match = String(text).replace(/\s+/g, ' ').match(/(\d[\d,]*(?:\.\d+)?)/);
    if (!match) {
      return null;
    }
    const plain = match[1].replace(/,/g, '');
    const value = Number(plain);
    if (!Number.isFinite(value) || value <= 0) {
      return null;
    }
    return plain;
  }

  // Trimmed text of the first element that matches one of the selectors, or null.
  textOf(selectors) {
    for (const selector of selectors) {
      const element = this.document.querySelector(selector);
      if (element && element.textContent && element.textContent.trim()) {
        return element.textContent.trim();
      }
    }
    return null;
  }

  firstElement(selectors) {
    for (const selector of selectors) {
      const element = this.document.querySelector(selector);
      if (element) {
        return element;
      }
    }
    return null;
  }

  // Reads <script type="application/ld+json"> blocks and returns the first schema.org Product, or null.
  // Structured data is read FIRST because it changes less often than the visible layout (SDS 3.4, SRS 6).
  readStructuredProduct() {
    const scripts = this.document.querySelectorAll('script[type="application/ld+json"]');
    for (const script of scripts) {
      let parsed;
      try {
        parsed = JSON.parse(script.textContent);
      } catch (notJson) {
        continue;
      }
      const candidates = Array.isArray(parsed) ? parsed : (parsed['@graph'] || [parsed]);
      for (const item of candidates) {
        if (item && (item['@type'] === 'Product' || (Array.isArray(item['@type']) && item['@type'].includes('Product')))) {
          return item;
        }
      }
    }
    return null;
  }

  // Price from a schema.org Product: offers.price, or offers.lowPrice for a price range. Null if unsure.
  static structuredPrice(product) {
    if (!product || !product.offers) {
      return null;
    }
    const offers = Array.isArray(product.offers) ? product.offers[0] : product.offers;
    if (!offers) {
      return null;
    }
    return BaseAdapter.parsePrice(offers.price !== undefined ? offers.price : offers.lowPrice);
  }
}
