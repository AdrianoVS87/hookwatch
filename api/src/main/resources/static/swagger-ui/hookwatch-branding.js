(function () {
  function applyBranding() {
    const topbar = document.querySelector('.swagger-ui .topbar-wrapper');
    if (!topbar || document.getElementById('hookwatch-branding')) return;

    const brand = document.createElement('div');
    brand.id = 'hookwatch-branding';
    brand.style.display = 'flex';
    brand.style.alignItems = 'center';
    brand.style.gap = '8px';
    brand.style.marginLeft = '12px';

    const logo = document.createElement('div');
    logo.textContent = '🪝';
    logo.style.fontSize = '20px';

    const text = document.createElement('strong');
    text.textContent = 'HookWatch API Docs';
    text.style.color = '#e2e8f0';
    text.style.letterSpacing = '0.2px';

    brand.appendChild(logo);
    brand.appendChild(text);
    topbar.appendChild(brand);
  }

  window.addEventListener('load', function () {
    applyBranding();
    setTimeout(applyBranding, 300);
  });
})();
