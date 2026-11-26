// DarazAdapter - reads Daraz product pages (https://www.daraz.pk/products/...-i<item>-s<sku>.html).
//
// !!! The CSS selectors below are UNVERIFIED. They follow Daraz's product page layout as commonly seen
// (classes such as "pdp-price_type_normal"), but they were NOT checked against a live page while writing
// this code. Before relying on them: open a Daraz product page, press F12, inspect the title, price,
// crossed-out price and discount ("-16%"), and fix the selector lists here. Save a copy of the page
// (Ctrl+S) into extension/test-pages/ so the adapter can be checked again later.
//
// Order of reading (SDS 3.4): structured data (ld+json) first, then the visible page elements.

class DarazAdapter extends BaseAdapter {
  static matches(loc) {
    const host = loc.hostname.toLowerCase();
    return host === 'www.daraz.pk' || host === 'daraz.pk';
  }

  // Product pages look like /products/haier-20l-...-i1969609715-s14056026528.html
  static idsFromPath(pathname) {
    const match = pathname.match(/-i(\d+)(?:-s(\d+))?\.html$/);
    if (!match) {
      return null;
    }
    return { item: match[1], sku: match[2] || null };
  }

  isProductPage() {
    return this.location.pathname.startsWith('/products/') && DarazAdapter.idsFromPath(this.location.pathname) !== null;
  }

  // One product row per item + variant: a different colour or size can have a different price.
  getSku() {
    const ids = DarazAdapter.idsFromPath(this.location.pathname);
    if (!ids) {
      return null;
    }
    return ids.sku ? ids.item + '-' + ids.sku : ids.item;
  }

  getTitle() {
    const structured = this.readStructuredProduct();
    if (structured && typeof structured.name === 'string' && structured.name.trim()) {
      return structured.name.trim();
    }
    return this.textOf(['.pdp-mod-product-badge-title', 'h1.pdp-mod-product-badge-title', 'h1']);
  }

  getCurrentPrice() {
    const fromStructured = BaseAdapter.structuredPrice(this.readStructuredProduct());
    if (fromStructured) {
      return fromStructured;
    }
    return BaseAdapter.parsePrice(this.textOf([
      '.pdp-product-price .pdp-price_type_normal',
      '.pdp-price_type_normal'
    ]));
  }

  getClaimedOriginalPrice() {
    const was = BaseAdapter.parsePrice(this.textOf([
      '.pdp-product-price .pdp-price_type_deleted',
      '.pdp-price_type_deleted'
    ]));
    const now = this.getCurrentPrice();
    if (!was || !now || Number(was) <= Number(now)) {
      return null; // a "was" price that is not higher than the price is not a discount claim
    }
    return was;
  }

  hasDiscountBadge() {
    const text = this.textOf(['.pdp-product-price__discount', '.pdp-product-price .pdp-product-price__discount']);
    return text !== null && /-?\d+\s*%/.test(text);
  }

  getImageUrl() {
    const structured = this.readStructuredProduct();
    if (structured && structured.image) {
      const image = Array.isArray(structured.image) ? structured.image[0] : structured.image;
      if (typeof image === 'string') {
        return image;
      }
    }
    const meta = this.document.querySelector('meta[property="og:image"]');
    return meta && meta.content ? meta.content : null;
  }

  getPriceElement() {
    return this.firstElement(['.pdp-product-price', '.pdp-price_type_normal']);
  }
}
