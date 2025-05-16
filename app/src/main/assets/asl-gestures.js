// ASL Alphabet Signs definitions using Fingerpose
// This file creates gesture descriptions for ASL alphabet signs

// Create ASL Gestures
const ASLGestures = {
    // Initialize with empty gestures
    gestures: [],
    
    // Create gesture descriptions for ASL alphabet
    initialize: function() {
        // A - Fist with thumb resting on the side
        const aSign = new fp.GestureDescription('A');
        
        // Thumb extended, touching first knuckle (side of index finger)
        aSign.addCurl(fp.Finger.Thumb, fp.FingerCurl.NoCurl, 1.0);
        aSign.addDirection(fp.Finger.Thumb, fp.FingerDirection.VerticalUp, 0.5);
        aSign.addDirection(fp.Finger.Thumb, fp.FingerDirection.DiagonalUpRight, 0.5);
        
        // All other fingers curled into fist
        for(let finger of [fp.Finger.Index, fp.Finger.Middle, fp.Finger.Ring, fp.Finger.Pinky]) {
            aSign.addCurl(finger, fp.FingerCurl.FullCurl, 1.0);
        }
        
        this.gestures.push(aSign);
        
        // B - Hand flat, fingers together, thumb across palm
        const bSign = new fp.GestureDescription('B');
        
        // Thumb tucked across palm
        bSign.addCurl(fp.Finger.Thumb, fp.FingerCurl.FullCurl, 1.0);
        
        // All other fingers straight and together
        for(let finger of [fp.Finger.Index, fp.Finger.Middle, fp.Finger.Ring, fp.Finger.Pinky]) {
            bSign.addCurl(finger, fp.FingerCurl.NoCurl, 1.0);
            bSign.addDirection(finger, fp.FingerDirection.VerticalUp, 0.7);
        }
        
        this.gestures.push(bSign);
        
        // C - Hand curved in C shape
        const cSign = new fp.GestureDescription('C');
        
        // All fingers in partial curl forming a C shape
        for(let finger of [fp.Finger.Thumb, fp.Finger.Index, fp.Finger.Middle, fp.Finger.Ring, fp.Finger.Pinky]) {
            cSign.addCurl(finger, fp.FingerCurl.HalfCurl, 0.8);
        }
        
        // Specific hand orientation for C
        cSign.addDirection(fp.Finger.Thumb, fp.FingerDirection.DiagonalUpLeft, 1.0);
        cSign.addDirection(fp.Finger.Index, fp.FingerDirection.DiagonalUpRight, 0.7);
        
        this.gestures.push(cSign);
        
        // Y - Thumb and pinky extended, other fingers curled
        const ySign = new fp.GestureDescription('Y');
        
        // Thumb extended
        ySign.addCurl(fp.Finger.Thumb, fp.FingerCurl.NoCurl, 1.0);
        ySign.addDirection(fp.Finger.Thumb, fp.FingerDirection.DiagonalUpLeft, 0.7);
        
        // Pinky extended
        ySign.addCurl(fp.Finger.Pinky, fp.FingerCurl.NoCurl, 1.0);
        ySign.addDirection(fp.Finger.Pinky, fp.FingerDirection.DiagonalUpRight, 0.7);
        
        // Other fingers curled
        for(let finger of [fp.Finger.Index, fp.Finger.Middle, fp.Finger.Ring]) {
            ySign.addCurl(finger, fp.FingerCurl.FullCurl, 1.0);
        }
        
        this.gestures.push(ySign);
        
        // Add more ASL gestures here...
        
        // I - Pinky extended, others curled
        const iSign = new fp.GestureDescription('I');
        
        // Pinky extended
        iSign.addCurl(fp.Finger.Pinky, fp.FingerCurl.NoCurl, 1.0);
        iSign.addDirection(fp.Finger.Pinky, fp.FingerDirection.VerticalUp, 0.7);
        
        // All other fingers curled
        for(let finger of [fp.Finger.Thumb, fp.Finger.Index, fp.Finger.Middle, fp.Finger.Ring]) {
            iSign.addCurl(finger, fp.FingerCurl.FullCurl, 1.0);
        }
        
        this.gestures.push(iSign);
        
        // L - L shape with thumb and index finger
        const lSign = new fp.GestureDescription('L');
        
        // Thumb extended sideways
        lSign.addCurl(fp.Finger.Thumb, fp.FingerCurl.NoCurl, 1.0);
        lSign.addDirection(fp.Finger.Thumb, fp.FingerDirection.HorizontalLeft, 0.7);
        lSign.addDirection(fp.Finger.Thumb, fp.FingerDirection.DiagonalUpLeft, 0.3);
        
        // Index extended upwards
        lSign.addCurl(fp.Finger.Index, fp.FingerCurl.NoCurl, 1.0);
        lSign.addDirection(fp.Finger.Index, fp.FingerDirection.VerticalUp, 0.7);
        
        // Other fingers curled
        for(let finger of [fp.Finger.Middle, fp.Finger.Ring, fp.Finger.Pinky]) {
            lSign.addCurl(finger, fp.FingerCurl.FullCurl, 1.0);
        }
        
        this.gestures.push(lSign);
        
        // O - Fingers curved to form O shape
        const oSign = new fp.GestureDescription('O');
        
        // All fingers in specific curl to form O shape
        for(let finger of [fp.Finger.Thumb, fp.Finger.Index, fp.Finger.Middle, fp.Finger.Ring, fp.Finger.Pinky]) {
            oSign.addCurl(finger, fp.FingerCurl.HalfCurl, 0.8);
        }
        
        // Specific positioning for O shape (thumb meeting index)
        oSign.addDirection(fp.Finger.Thumb, fp.FingerDirection.DiagonalUpRight, 1.0);
        oSign.addDirection(fp.Finger.Index, fp.FingerDirection.DiagonalUpLeft, 0.7);
        
        this.gestures.push(oSign);
    },
    
    // Get all initialized gestures
    getGestures: function() {
        if (this.gestures.length === 0) {
            this.initialize();
        }
        return this.gestures;
    }
};

// Initialize ASL gestures when the page loads
window.addEventListener('load', () => {
    // Will initialize when app.js calls getGestures()
    console.log('ASL Gestures ready to initialize');
});
