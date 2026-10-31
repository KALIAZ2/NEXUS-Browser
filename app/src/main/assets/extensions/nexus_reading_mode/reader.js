(function () {
  "use strict";
  try {
    const style = document.createElement("style");
    style.id = "nexus-reading-enhancer";
    style.textContent = `
      html {
        scroll-behavior: smooth;
        -webkit-text-size-adjust: 100%;
      }
      pre, code {
        overflow-x: auto;
        max-width: 100%;
      }
    `;
    (document.head || document.documentElement).appendChild(style);
  } catch (_) {}
})();
