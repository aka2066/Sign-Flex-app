/**
 * Speech Manager
 * Provides text-to-speech functionality for SignFlex
 */

class SpeechManager {
  constructor() {
    this.speaking = false;
    this.queue = [];
    this.initialized = false;
    this.enabled = true;
    this.voice = null;
    this.pitch = 1.0;
    this.rate = 1.0;
    this.volume = 1.0;
  }

  // Initialize the speech synthesis
  init() {
    if (!('speechSynthesis' in window)) {
      console.error('Speech synthesis not supported');
      this.initialized = false;
      return false;
    }

    // Wait for voices to be loaded
    if (window.speechSynthesis.getVoices().length === 0) {
      window.speechSynthesis.addEventListener('voiceschanged', () => {
        this.loadVoices();
      });
    } else {
      this.loadVoices();
    }

    this.initialized = true;
    return true;
  }

  // Load available voices and select a default one
  loadVoices() {
    this.voices = window.speechSynthesis.getVoices();
    
    // Try to find a good default voice (preferably female English voice)
    const defaultVoice = this.voices.find(voice => 
      voice.lang.includes('en') && voice.name.includes('Female')
    ) || this.voices.find(voice => 
      voice.lang.includes('en')
    ) || this.voices[0];
    
    this.setVoice(defaultVoice);
    console.log(`Loaded ${this.voices.length} voices, default: ${defaultVoice.name}`);
  }

  // Enable/disable speech
  setEnabled(enabled) {
    this.enabled = enabled;
    if (!enabled && this.speaking) {
      this.stop();
    }
    return this.enabled;
  }

  // Toggle speech on/off
  toggleSpeech() {
    return this.setEnabled(!this.enabled);
  }

  // Get available voices
  getVoices() {
    return this.voices || [];
  }

  // Set voice by voice object
  setVoice(voice) {
    this.voice = voice;
  }

  // Set voice by index
  setVoiceByIndex(index) {
    if (this.voices && index >= 0 && index < this.voices.length) {
      this.voice = this.voices[index];
      return true;
    }
    return false;
  }

  // Set speech parameters
  setParams(params = {}) {
    if (params.pitch !== undefined) this.pitch = params.pitch;
    if (params.rate !== undefined) this.rate = params.rate;
    if (params.volume !== undefined) this.volume = params.volume;
  }

  // Speak text
  speak(text, interrupt = false) {
    if (!this.initialized) {
      this.init();
    }
    
    if (!this.enabled || !text) {
      return false;
    }

    // If interrupt is true, stop current speech
    if (interrupt && this.speaking) {
      this.stop();
    }

    // If already speaking and not interrupting, add to queue
    if (this.speaking && !interrupt) {
      this.queue.push(text);
      return true;
    }

    const utterance = new SpeechSynthesisUtterance(text);
    
    if (this.voice) {
      utterance.voice = this.voice;
    }
    
    utterance.pitch = this.pitch;
    utterance.rate = this.rate;
    utterance.volume = this.volume;
    
    utterance.onstart = () => {
      this.speaking = true;
    };
    
    utterance.onend = () => {
      this.speaking = false;
      // Check if there are more items in the queue
      if (this.queue.length > 0) {
        const nextText = this.queue.shift();
        this.speak(nextText);
      }
    };
    
    utterance.onerror = (event) => {
      console.error('Speech synthesis error:', event);
      this.speaking = false;
    };
    
    try {
      window.speechSynthesis.speak(utterance);
      return true;
    } catch (error) {
      console.error('Error speaking:', error);
      return false;
    }
  }

  // Speak a letter
  speakLetter(letter) {
    if (!letter) return false;
    
    // Convert to uppercase and ensure it's a single character or special word
    let text = letter.toString().toUpperCase();
    
    // Special handling for certain letters or phrases
    if (text === 'I LOVE YOU') {
      return this.speak('I love you');
    }
    
    // For single letters, add pronunciation clarity
    if (text.length === 1) {
      // Add "The letter X" to improve clarity for single letters
      text = `The letter ${text}`;
    }
    
    return this.speak(text);
  }

  // Stop speaking and clear queue
  stop() {
    this.queue = [];
    window.speechSynthesis.cancel();
    this.speaking = false;
  }

  // Pause speaking
  pause() {
    if (this.speaking) {
      window.speechSynthesis.pause();
    }
  }

  // Resume speaking
  resume() {
    if (this.speaking) {
      window.speechSynthesis.resume();
    }
  }
}

// Create a singleton instance
const speechManager = new SpeechManager();
export default speechManager;
