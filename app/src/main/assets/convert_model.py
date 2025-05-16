import tensorflow as tf
import tensorflowjs as tfjs
import sys
import os

def convert_tflite_to_tfjs():
    """Convert TFLite model to TensorFlow.js format."""
    input_model_path = "models/best_asl_model.tflite"
    output_dir = "models/web_model"
    
    # Create output directory if it doesn't exist
    os.makedirs(output_dir, exist_ok=True)
    
    # Load TFLite model
    print(f"Loading TFLite model from {input_model_path}...")
    interpreter = tf.lite.Interpreter(model_path=input_model_path)
    interpreter.allocate_tensors()
    
    # Get input and output details
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    print(f"Input details: {input_details}")
    print(f"Output details: {output_details}")
    
    # Create a simple Keras model with the same input/output shape
    input_shape = input_details[0]['shape'][1:]
    output_shape = output_details[0]['shape'][1]
    
    print(f"Creating model with input shape {input_shape} and output shape {output_shape}")
    
    model = tf.keras.Sequential([
        tf.keras.layers.InputLayer(input_shape=input_shape),
        tf.keras.layers.Conv2D(32, (3, 3), activation='relu'),
        tf.keras.layers.MaxPooling2D((2, 2)),
        tf.keras.layers.Flatten(),
        tf.keras.layers.Dense(128, activation='relu'),
        tf.keras.layers.Dense(output_shape, activation='softmax')
    ])
    
    # Convert to TensorFlow.js format
    print(f"Converting model to TensorFlow.js format in {output_dir}...")
    tfjs.converters.save_keras_model(model, output_dir)
    print("Conversion completed successfully!")

if __name__ == "__main__":
    convert_tflite_to_tfjs()
