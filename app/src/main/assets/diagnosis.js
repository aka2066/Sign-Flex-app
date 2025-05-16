/**
 * Diagnostic script to help identify UI issues
 */

// Function to check DOM elements
function checkElements() {
  const elementsToCheck = [
    'login-screen',
    'home-screen',
    'asl-screen', 
    'ble-screen',
    'start-asl-btn',
    'start-ble-btn'
  ];
  
  console.log('--- DOM ELEMENT DIAGNOSTICS ---');
  elementsToCheck.forEach(id => {
    const element = document.getElementById(id);
    console.log(`Element #${id}: ${element ? 'FOUND' : 'MISSING'} ${element ? (element.classList.contains('hidden') ? '(HIDDEN)' : '(VISIBLE)') : ''}`);
  });
}

// Check for script loading errors
function checkScripts() {
  const scripts = [
    { name: 'auth.js', loaded: typeof auth !== 'undefined' },
    { name: 'login-screen.js', loaded: typeof loginScreen !== 'undefined' },
    { name: 'ble-manager.js', loaded: typeof bleManager !== 'undefined' }
  ];
  
  console.log('--- SCRIPT LOADING DIAGNOSTICS ---');
  scripts.forEach(script => {
    console.log(`Script ${script.name}: ${script.loaded ? 'LOADED' : 'NOT LOADED'}`);
  });
}

// Run diagnostics when page loads
window.addEventListener('DOMContentLoaded', () => {
  console.log('RUNNING DIAGNOSTICS...');
  setTimeout(() => {
    checkElements();
    checkScripts();
    console.log('DIAGNOSTICS COMPLETE');
  }, 500); // Slight delay to ensure other scripts have had a chance to run
});

// Export for module compatibility
export default { checkElements, checkScripts };
