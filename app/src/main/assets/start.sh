#!/bin/bash
# SignFlex Startup Script

# Start MongoDB authentication server
echo "Starting MongoDB authentication server..."
cd server && node auth-server.js --port 3001 &
AUTH_PID=$!

# Start HTTP server for web app with proper CORS headers
echo "Starting web server..."
npx http-server -p 8090 --cors -c-1 &
HTTP_PID=$!

echo "SignFlex is running!"
echo "Access the web app at: http://localhost:8090"
echo ""
echo "Press Ctrl+C to stop all servers..."

# Handle script termination
trap "echo 'Stopping servers...'; kill $AUTH_PID $HTTP_PID; exit" INT TERM

# Keep script running
wait
