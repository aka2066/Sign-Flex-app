/**
 * Flex Sensor Database Manager
 * Handles storage and retrieval of flex sensor data using IndexedDB
 */

class FlexDBManager {
  constructor() {
    this.dbName = 'SignFlexDB';
    this.storeName = 'flexSensorData';
    this.db = null;
    this.initDB();
  }

  // Initialize the database
  async initDB() {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.dbName, 1);
      
      request.onerror = (event) => {
        console.error('Error opening database:', event.target.error);
        reject(event.target.error);
      };
      
      request.onsuccess = (event) => {
        this.db = event.target.result;
        console.log('Database opened successfully');
        resolve(this.db);
      };
      
      request.onupgradeneeded = (event) => {
        const db = event.target.result;
        
        // Create object store for flex sensor data
        // Use timestamp as key path
        const objectStore = db.createObjectStore(this.storeName, { keyPath: 'timestamp' });
        
        // Create indexes
        objectStore.createIndex('letter', 'letter', { unique: false });
        objectStore.createIndex('flexValues', 'flexValues', { unique: false });
        objectStore.createIndex('userId', 'userId', { unique: false });
        
        console.log('Database setup complete');
      };
    });
  }

  // Save flex sensor data with associated letter
  async saveFlexData(letter, flexValues, userId) {
    return new Promise((resolve, reject) => {
      if (!this.db) {
        return this.initDB().then(() => this.saveFlexData(letter, flexValues, userId));
      }
      
      const transaction = this.db.transaction([this.storeName], 'readwrite');
      const store = transaction.objectStore(this.storeName);
      
      const data = {
        timestamp: Date.now(),
        letter: letter,
        flexValues: flexValues,
        userId: userId
      };
      
      const request = store.add(data);
      
      request.onsuccess = () => {
        console.log(`Flex data for letter '${letter}' saved successfully`);
        resolve(true);
      };
      
      request.onerror = (event) => {
        console.error('Error saving flex data:', event.target.error);
        reject(event.target.error);
      };
    });
  }

  // Get all saved letters for a user
  async getLetterHistory(userId) {
    return new Promise((resolve, reject) => {
      if (!this.db) {
        return this.initDB().then(() => this.getLetterHistory(userId));
      }
      
      const transaction = this.db.transaction([this.storeName], 'readonly');
      const store = transaction.objectStore(this.storeName);
      const index = store.index('userId');
      
      const request = index.getAll(userId);
      
      request.onsuccess = () => {
        const data = request.result;
        console.log(`Retrieved ${data.length} flex data records for user`);
        resolve(data);
      };
      
      request.onerror = (event) => {
        console.error('Error retrieving flex data:', event.target.error);
        reject(event.target.error);
      };
    });
  }

  // Get letter suggestions based on flex sensor values
  async getLetterSuggestion(flexValues, userId, threshold = 0.15) {
    return new Promise((resolve, reject) => {
      if (!this.db) {
        return this.initDB().then(() => this.getLetterSuggestion(flexValues, userId, threshold));
      }
      
      this.getLetterHistory(userId).then((records) => {
        if (records.length === 0) {
          resolve(null);
          return;
        }
        
        // Calculate similarity between current flex values and stored values
        const similarities = records.map(record => {
          const storedValues = record.flexValues;
          
          // Skip if array lengths don't match
          if (storedValues.length !== flexValues.length) {
            return { letter: record.letter, similarity: 0 };
          }
          
          // Calculate Euclidean distance
          let sumSquaredDiff = 0;
          for (let i = 0; i < flexValues.length; i++) {
            sumSquaredDiff += Math.pow(flexValues[i] - storedValues[i], 2);
          }
          const distance = Math.sqrt(sumSquaredDiff);
          
          // Convert distance to similarity (1 = identical, 0 = completely different)
          const similarity = 1 / (1 + distance);
          
          return { letter: record.letter, similarity };
        });
        
        // Find best match
        const bestMatch = similarities.reduce((best, current) => {
          return current.similarity > best.similarity ? current : best;
        }, { letter: null, similarity: 0 });
        
        // Return match if it's above threshold
        if (bestMatch.similarity >= threshold) {
          resolve(bestMatch);
        } else {
          resolve(null);
        }
      }).catch(reject);
    });
  }

  // Clear all data for a user
  async clearUserData(userId) {
    return new Promise((resolve, reject) => {
      if (!this.db) {
        return this.initDB().then(() => this.clearUserData(userId));
      }
      
      const transaction = this.db.transaction([this.storeName], 'readwrite');
      const store = transaction.objectStore(this.storeName);
      const index = store.index('userId');
      
      const request = index.openCursor(userId);
      
      request.onsuccess = (event) => {
        const cursor = event.target.result;
        if (cursor) {
          cursor.delete();
          cursor.continue();
        } else {
          console.log(`Cleared all flex data for user ${userId}`);
          resolve(true);
        }
      };
      
      request.onerror = (event) => {
        console.error('Error clearing flex data:', event.target.error);
        reject(event.target.error);
      };
    });
  }
}

// Create a singleton instance
const flexDBManager = new FlexDBManager();
export default flexDBManager;
