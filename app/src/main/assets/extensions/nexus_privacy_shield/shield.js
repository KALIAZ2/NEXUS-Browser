(function () {
  "use strict";
  const TRACKING_PARAMS = [
    "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content",
    "fbclid", "gclid", "msclkid", "mc_cid", "mc_eid", "_ga", "igshid"
  ];

  function cleanCurrentUrl() {
    try {
      const url = new URL(window.location.href);
      let modified = false;
      TRACKING_PARAMS.forEach((param) => {
        if (url.searchParams.has(param)) {
          url.searchParams.delete(param);
          modified = true;
        }
      });
      if (modified && window.history && window.history.replaceState) {
        window.history.replaceState({}, document.title, url.toString());
      }
    } catch (_) {}
  }

  cleanCurrentUrl();

  document.addEventListener("DOMContentLoaded", () => {
    const style = document.createElement("style");
    style.id = "nexus-privacy-shield-css";
    style.textContent = `
      [id*="cookie-banner"], [class*="cookie-banner"],
      [id*="consent-banner"], [class*="gdpr-banner"] {
        display: none !important;
      }
    `;
    (document.head || document.documentElement).appendChild(style);
  });
})();
