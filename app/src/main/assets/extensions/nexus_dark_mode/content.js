(function () {
  "use strict";
  try {
    const existing = document.getElementById("nexus-dark-mode-style");
    if (existing) return;

    const bg = window.getComputedStyle(document.body || document.documentElement).backgroundColor;
    // Check if page is already dark
    const rgbMatch = bg && bg.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)/);
    if (rgbMatch) {
      const r = parseInt(rgbMatch[1], 10);
      const g = parseInt(rgbMatch[2], 10);
      const b = parseInt(rgbMatch[3], 10);
      const luminance = 0.299 * r + 0.587 * g + 0.114 * b;
      if (luminance > 0 && luminance < 80) {
        return; // Page already uses a dark background
      }
    }

    const style = document.createElement("style");
    style.id = "nexus-dark-mode-style";
    style.textContent = `
      html {
        filter: invert(0.92) hue-rotate(180deg) !important;
        background-color: #090B10 !important;
      }
      img, video, picture, canvas, iframe, svg {
        filter: invert(1) hue-rotate(180deg) !important;
      }
    `;
    (document.head || document.documentElement).appendChild(style);
  } catch (e) {
    console.warn("[NEXUS Dark Mode]", e);
  }
})();
