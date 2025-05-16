// DOM Elements
const video = document.getElementById('video');
const canvas = document.getElementById('canvas');
const result = document.getElementById('result');
const modelStatus = document.getElementById('model-status');

// Canvas context
const ctx = canvas.getContext('2d');

// ASL Recognition Constants
const IMAGE_SIZE = 224;
const STABILITY_THRESHOLD = 3;
const CONFIDENCE_THRESHOLD = 7.5; // Minimum confidence score to accept a gesture

// State
let handposeModel = null;
let isModelLoaded = false;
let lastGestures = [];
let videoStream = null;
let recognitionInterval = null;

// Function to initialize ASL functionality
async function initASL() {
  console.log("Initializing ASL functionality");
  initCamera();
  if (!isModelLoaded) {
    loadModel();
  }
}

// Camera functions
async function initCamera() {
  try {
    const constraints = {
      video: {
        width: { ideal: 640 },
        height: { ideal: 480 },
        facingMode: 'user'
      }
    };
    
    videoStream = await navigator.mediaDevices.getUserMedia(constraints);
    video.srcObject = videoStream;
    
    // Wait for video to be ready
    return new Promise((resolve) => {
      video.onloadedmetadata = () => {
        // Resize canvas to match video dimensions
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;
        console.log("Camera initialized");
        resolve();
      };
    });
  } catch (error) {
    console.error('Error initializing camera:', error);
    result.textContent = 'Camera error: ' + error.message;
  }
}

function stopCamera() {
  if (videoStream) {
    const tracks = videoStream.getTracks();
    tracks.forEach(track => track.stop());
    videoStream = null;
  }
  
  if (recognitionInterval) {
    clearInterval(recognitionInterval);
    recognitionInterval = null;
  }
  
  // Clear canvas
  if (ctx) {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  }
  
  // Reset result
  if (result) {
    result.textContent = 'Waiting...';
  }
}

// TensorFlow.js model functions
async function loadModel() {
  try {
    modelStatus.textContent = 'Model: Loading...';
    // Load handpose model
    handposeModel = await handpose.load();
    isModelLoaded = true;
    modelStatus.textContent = 'Model: Loaded';
    console.log('Handpose model loaded');
    
    // Start prediction loop
    recognitionInterval = setInterval(predictLoop, 100);
    
    return handposeModel;
  } catch (error) {
    console.error('Error loading handpose model:', error);
    modelStatus.textContent = 'Model: Error loading';
  }
}

async function predictLoop() {
  if (!videoStream || !handposeModel || !isModelLoaded) {
    return;
  }
  
  try {
    const predictions = await handposeModel.estimateHands(video);
    
    // Draw video frame
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    
    if (predictions.length > 0) {
      // We have at least one hand detected
      drawHandLandmarks(predictions[0]);
      processFrame(predictions[0]);
    }
  } catch (error) {
    console.error('Error in prediction loop:', error);
  }
}

function processFrame(hand) {
  if (!hand.landmarks) return;
  
  const landmarks = hand.landmarks;
  
  // Fingerpose gesture estimation
  const estimator = new fp.GestureEstimator([
    ASLGestures.aSign,
    ASLGestures.bSign,
    ASLGestures.cSign,
    ASLGestures.dSign,
    ASLGestures.eSign,
    ASLGestures.fSign,
    ASLGestures.iLoveYouSign,
    ASLGestures.thumbsUpSign,
    ASLGestures.thumbsDownSign
  ]);
  
  const gesture = recognizeGesture(landmarks);
  
  if (gesture) {
    // Add to last gestures for stability
    lastGestures.push(gesture);
    
    // Keep only recent gestures for stability check
    if (lastGestures.length > STABILITY_THRESHOLD) {
      lastGestures.shift();
    }
    
    // Check if we have a stable gesture
    if (lastGestures.length === STABILITY_THRESHOLD) {
      const mostCommon = findMostCommonGesture(lastGestures);
      result.textContent = mostCommon;
    }
  }
}

function recognizeGesture(handLandmarks) {
  // Use fingerpose for estimation
  const estimator = new fp.GestureEstimator([
    ASLGestures.aSign,
    ASLGestures.bSign,
    ASLGestures.cSign,
    ASLGestures.dSign,
    ASLGestures.eSign,
    ASLGestures.fSign,
    ASLGestures.iLoveYouSign,
    ASLGestures.thumbsUpSign,
    ASLGestures.thumbsDownSign
  ]);
  
  const gestures = estimator.estimate(handLandmarks, CONFIDENCE_THRESHOLD);
  
  if (gestures.gestures.length > 0) {
    // Find gesture with highest confidence
    const confidence = gestures.gestures.map(g => g.score);
    const mostConfidentGesture = gestures.gestures[confidence.indexOf(Math.max(...confidence))];
    
    return mostConfidentGesture.name;
  }
  
  return null;
}

// Find most common gesture from array
function findMostCommonGesture(arr) {
  return arr.sort((a, b) =>
    arr.filter(v => v === a).length - arr.filter(v => v === b).length
  ).pop();
}

// Draw hand detection visualization
function drawHandLandmarks(hand) {
  const landmarks = hand.landmarks;
  
  // Draw keypoints
  for (let i = 0; i < landmarks.length; i++) {
    const [x, y, z] = landmarks[i];
    
    ctx.beginPath();
    ctx.arc(x, y, 5, 0, 2 * Math.PI);
    ctx.fillStyle = 'red';
    ctx.fill();
  }
  
  // Draw connections
  const connections = [
    // Thumb
    [0, 1], [1, 2], [2, 3], [3, 4],
    // Index finger
    [0, 5], [5, 6], [6, 7], [7, 8],
    // Middle finger
    [0, 9], [9, 10], [10, 11], [11, 12],
    // Ring finger
    [0, 13], [13, 14], [14, 15], [15, 16],
    // Pinky
    [0, 17], [17, 18], [18, 19], [19, 20],
    // Palm
    [0, 5], [5, 9], [9, 13], [13, 17]
  ];
  
  // Draw lines
  ctx.strokeStyle = 'blue';
  ctx.lineWidth = 2;
  
  for (let i = 0; i < connections.length; i++) {
    const [idx1, idx2] = connections[i];
    ctx.beginPath();
    ctx.moveTo(landmarks[idx1][0], landmarks[idx1][1]);
    ctx.lineTo(landmarks[idx2][0], landmarks[idx2][1]);
    ctx.stroke();
  }
}

// Export functions for use in main.js
export { initASL, stopCamera, isModelLoaded, loadModel };
