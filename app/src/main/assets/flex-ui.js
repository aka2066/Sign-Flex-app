/**
 * Flex Sensor UI Manager
 * Provides UI components for flex sensor data management
 */

import flexDBManager from './flex-db.js';
import speechManager from './speech-manager.js';

class FlexUIManager {
  constructor() {
    this.currentFlexValues = [];
    this.currentUserId = null;
    this.letterHistoryElement = null;
    this.letterSuggestionElement = null;
    this.speechEnabled = true;
  }
  
  // Initialize UI components
  init(userId) {
    this.currentUserId = userId;
    
    // Initialize speech manager
    speechManager.init();
    
    // Create UI elements if they don't exist
    this.createUIElements();
    
    // Load existing letter history
    this.loadLetterHistory();
  }
  
  // Create UI elements for flex sensor interaction
  createUIElements() {
    // Check if we already created the UI
    if (document.getElementById('flex-data-container')) {
      return;
    }
    
    // Get BLE screen
    const bleScreen = document.getElementById('ble-screen');
    if (!bleScreen) {
      console.error('BLE screen not found');
      return;
    }
    
    // Create flex data container
    const flexContainer = document.createElement('div');
    flexContainer.id = 'flex-data-container';
    flexContainer.className = 'flex-data-container';
    
    // Create flex sensor data display
    const flexDataDisplay = document.createElement('div');
    flexDataDisplay.className = 'sensor-data';
    flexDataDisplay.innerHTML = `
      <h3>Flex Sensor Data</h3>
      <div class="sensor">
        <span class="sensor-label">Current Values:</span>
        <span id="flex-values" class="sensor-value">-</span>
      </div>
    `;
    
    // Create letter recording section
    const letterSection = document.createElement('div');
    letterSection.className = 'letter-recording-section';
    letterSection.innerHTML = `
      <h3>Save Letter Gesture</h3>
      <div class="letter-input-container">
        <input type="text" id="letter-input" maxlength="1" placeholder="Enter letter">
        <button id="save-letter-btn" class="action-btn">Save</button>
      </div>
      <div class="letter-suggestion">
        <span>Suggestion: </span>
        <span id="letter-suggestion">-</span>
        <button id="speak-suggestion-btn" class="icon-btn" title="Speak letter"><i class="fas fa-volume-up"></i>🔊</button>
      </div>
    `;
    
    // Create letter history section
    const historySection = document.createElement('div');
    historySection.className = 'letter-history-section';
    historySection.innerHTML = `
      <h3>Saved Letters</h3>
      <div id="letter-history" class="letter-history"></div>
      <div class="letter-history-controls">
        <button id="clear-history-btn" class="action-btn">Clear History</button>
        <label class="speech-toggle">
          <input type="checkbox" id="speech-toggle" checked>
          <span class="toggle-label">Text-to-Speech</span>
        </label>
      </div>
    `;
    
    // Add elements to container
    flexContainer.appendChild(flexDataDisplay);
    flexContainer.appendChild(letterSection);
    flexContainer.appendChild(historySection);
    
    // Add container to BLE screen
    // Insert it before the data-container if it exists
    const dataContainer = document.getElementById('data-container');
    if (dataContainer) {
      bleScreen.insertBefore(flexContainer, dataContainer);
    } else {
      bleScreen.appendChild(flexContainer);
    }
    
    // Get references to elements
    this.letterHistoryElement = document.getElementById('letter-history');
    this.letterSuggestionElement = document.getElementById('letter-suggestion');
    
    // Add event listeners
    document.getElementById('save-letter-btn').addEventListener('click', () => this.saveCurrentLetter());
    document.getElementById('clear-history-btn').addEventListener('click', () => this.clearLetterHistory());
    document.getElementById('speak-suggestion-btn').addEventListener('click', () => this.speakCurrentSuggestion());
    
    // Speech toggle
    const speechToggle = document.getElementById('speech-toggle');
    speechToggle.addEventListener('change', (e) => {
      this.speechEnabled = e.target.checked;
      speechManager.setEnabled(this.speechEnabled);
    });
    
    // Add CSS
    this.addStyles();
  }
  
  // Add CSS styles
  addStyles() {
    // Check if styles already exist
    if (document.getElementById('flex-ui-styles')) {
      return;
    }
    
    const style = document.createElement('style');
    style.id = 'flex-ui-styles';
    style.textContent = `
      .flex-data-container {
        margin-top: 20px;
        padding: 15px;
        background-color: rgba(255, 255, 255, 0.1);
        border-radius: 8px;
      }
      
      .letter-recording-section, .letter-history-section {
        margin-top: 15px;
      }
      
      .letter-input-container {
        display: flex;
        gap: 10px;
        margin-bottom: 10px;
      }
      
      #letter-input {
        padding: 8px;
        border-radius: 4px;
        border: 1px solid #ddd;
        background-color: rgba(255, 255, 255, 0.8);
        font-size: 16px;
        width: 60px;
        text-align: center;
        text-transform: uppercase;
      }
      
      .letter-suggestion {
        margin-top: 10px;
        font-size: 14px;
        display: flex;
        align-items: center;
        gap: 8px;
      }
      
      .icon-btn {
        background: none;
        border: none;
        cursor: pointer;
        font-size: 18px;
        padding: 0;
        color: #2196F3;
      }
      
      .letter-history {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-top: 10px;
        margin-bottom: 15px;
      }
      
      .letter-item {
        display: flex;
        justify-content: center;
        align-items: center;
        width: 40px;
        height: 40px;
        background-color: #2196F3;
        color: white;
        border-radius: 4px;
        font-weight: bold;
        cursor: pointer;
      }
      
      .letter-history-controls {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
      
      .speech-toggle {
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;
      }
      
      .toggle-label {
        font-size: 14px;
      }
    `;
    
    document.head.appendChild(style);
  }
  
  // Update current flex values
  updateFlexValues(flexValues) {
    this.currentFlexValues = flexValues;
    
    // Update UI
    const flexValuesElement = document.getElementById('flex-values');
    if (flexValuesElement) {
      flexValuesElement.textContent = flexValues.map(v => v.toFixed(2)).join(', ');
    }
    
    // Check for letter suggestion
    this.updateLetterSuggestion();
  }
  
  // Update letter suggestion based on current flex values
  async updateLetterSuggestion() {
    if (!this.currentUserId || this.currentFlexValues.length === 0) {
      return;
    }
    
    try {
      const suggestion = await flexDBManager.getLetterSuggestion(
        this.currentFlexValues, 
        this.currentUserId
      );
      
      if (suggestion && this.letterSuggestionElement) {
        const similarity = Math.round(suggestion.similarity * 100);
        this.letterSuggestionElement.textContent = 
          `${suggestion.letter} (${similarity}% match)`;
        
        // Speak the letter if enabled and similarity is high enough
        if (this.speechEnabled && similarity >= 85) {
          // Use the actual letter, not the whole text with similarity
          speechManager.speakLetter(suggestion.letter);
        }
      } else if (this.letterSuggestionElement) {
        this.letterSuggestionElement.textContent = '- (no match)';
      }
    } catch (error) {
      console.error('Error getting letter suggestion:', error);
    }
  }
  
  // Speak current letter suggestion
  speakCurrentSuggestion() {
    const suggestionText = this.letterSuggestionElement?.textContent;
    if (!suggestionText || suggestionText.includes('no match')) {
      return;
    }
    
    // Extract just the letter part from the suggestion text
    const letterMatch = suggestionText.match(/^([A-Z0-9]+|\w+\s+\w+\s+\w+)/);
    if (letterMatch && letterMatch[1]) {
      speechManager.speakLetter(letterMatch[1]);
    }
  }
  
  // Save current flex values as a letter
  async saveCurrentLetter() {
    if (!this.currentUserId || this.currentFlexValues.length === 0) {
      alert('No flex sensor data available');
      return;
    }
    
    const letterInput = document.getElementById('letter-input');
    if (!letterInput) {
      return;
    }
    
    const letter = letterInput.value.trim().toUpperCase();
    if (!letter) {
      alert('Please enter a letter');
      return;
    }
    
    try {
      await flexDBManager.saveFlexData(letter, this.currentFlexValues, this.currentUserId);
      
      // Clear input and reload history
      letterInput.value = '';
      this.loadLetterHistory();
      
      // Speak the saved letter
      if (this.speechEnabled) {
        speechManager.speakLetter(letter);
      }
      
      // Show success message
      alert(`Letter '${letter}' saved successfully`);
    } catch (error) {
      console.error('Error saving letter:', error);
      alert('Failed to save letter');
    }
  }
  
  // Load letter history from database
  async loadLetterHistory() {
    if (!this.currentUserId || !this.letterHistoryElement) {
      return;
    }
    
    try {
      const history = await flexDBManager.getLetterHistory(this.currentUserId);
      
      // Clear existing history
      this.letterHistoryElement.innerHTML = '';
      
      // Track already displayed letters
      const displayedLetters = new Set();
      
      // Add letter items to history
      history.forEach(item => {
        // Only show each letter once
        if (!displayedLetters.has(item.letter)) {
          const letterItem = document.createElement('div');
          letterItem.className = 'letter-item';
          letterItem.textContent = item.letter;
          letterItem.addEventListener('click', () => {
            if (this.speechEnabled) {
              speechManager.speakLetter(item.letter);
            }
          });
          this.letterHistoryElement.appendChild(letterItem);
          
          displayedLetters.add(item.letter);
        }
      });
      
      // Show message if no letters
      if (history.length === 0) {
        this.letterHistoryElement.innerHTML = '<p>No saved letters yet</p>';
      }
    } catch (error) {
      console.error('Error loading letter history:', error);
    }
  }
  
  // Clear all letter history
  async clearLetterHistory() {
    if (!this.currentUserId) {
      return;
    }
    
    if (confirm('Are you sure you want to clear all saved letters?')) {
      try {
        await flexDBManager.clearUserData(this.currentUserId);
        this.loadLetterHistory();
        alert('Letter history cleared');
      } catch (error) {
        console.error('Error clearing letter history:', error);
        alert('Failed to clear letter history');
      }
    }
  }
}

// Create a singleton instance
const flexUIManager = new FlexUIManager();
export default flexUIManager;
