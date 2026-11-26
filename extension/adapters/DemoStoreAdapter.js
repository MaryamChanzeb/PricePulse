// DemoStoreAdapter - reads the local DEMO store pages served by the Price Pulse server
// (http://localhost:8080/demo-store/*.html). Used in class to show every verdict type (SRS 6, "Early data").
// It overrides the same BaseAdapter methods as DarazAdapter, for a different page layout.

class DemoStoreAdapter extends BaseAdapter {
  static matches(loc) {
    return loc.hostname === 'localhost' && loc.port === '8080' && loc.pathname.startsWith('/demo-store/');
  }

  productRoot() {
    return this.document.querySelector('[data-product-sku]');
  }

  isProductPage() {
    return this.productRoot() !== null;
  }

  getSku() {
    const root = this.productRoot();
    const sku = root ? root.getAttribute('data-product-sku') : null;
    return sku && sku.trim() ? sku.trim() : null;
  }

  getTitle() {
    return this.textOf(['.product-title']);
  }

  getCurrentPrice() {
    return BaseAdapter.parsePrice(this.textOf(['.price-now']));
  }

  getClaimedOriginalPrice() {
    const was = BaseAdapter.parsePrice(this.textOf(['.price-was']));
    const now = this.getCurrentPrice();
    if (!was || !now || Number(was) <= Number(now)) {
      return null;
    }
    return was;
  }

  hasDiscountBadge() {
    return this.document.querySelector('.discount-badge') !== null;
  }

  getPriceElement() {
    return this.firstElement(['.price-box']);
  }
}
