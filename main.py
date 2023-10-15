import base64
from PIL import Image as PILImage
from io import BytesIO
from google.cloud import aiplatform
from google.cloud import storage
from datetime import datetime
from google.protobuf import struct_pb2
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np
import json
from vertexai.vision_models import ImageTextModel, Image
import vertexai



def classify_banana(request):
    print('Starting classify_banana function')
    
    # Initialize Vertex AI client
    project = "yourprojecthere"
    location = "us-central1"
    client = aiplatform.gapic.PredictionServiceClient(client_options={"api_endpoint": "us-central1-aiplatform.googleapis.com"})
    endpoint = f"projects/{project}/locations/{location}/publishers/google/models/multimodalembedding@001"
    
    print('Vertex AI client initialized')

    vertexai.init(project=project, location=location)
    model = ImageTextModel.from_pretrained("imagetext@001")

    
    # Get image from the request
    image_bytes = request.files['image'].read()
    encoded_content = base64.b64encode(image_bytes).decode("utf-8")
    
    print('Received and encoded image')
    
    def get_or_create_bucket(bucket_name):
        storage_client = storage.Client()
        bucket = storage_client.lookup_bucket(bucket_name)
        if bucket is None:
            bucket = storage_client.create_bucket(bucket_name)
        return bucket

    # Get or create bucket
    bucket = get_or_create_bucket('yourbuckethere')

    # Generate a unique blob name
    blob_name = f"your-image-name-{datetime.now().strftime('%Y%m%d%H%M%S')}.jpeg"
    blob = bucket.blob(blob_name)

    # Upload image bytes
    blob.upload_from_string(image_bytes, content_type='image/jpeg')

    # Make blob public and get its URL
    blob.make_public()
    public_url = blob.public_url

    # Use the URL for further processing
    
    # Generate image embeddings
    instance = struct_pb2.Struct()
    instance.fields['image'].struct_value.fields['bytesBase64Encoded'].string_value = encoded_content
    response = client.predict(endpoint=endpoint, instances=[instance])
    image_embedding = response.predictions[0]['imageEmbedding']
    
    print('Generated image embeddings')
         

    # Convert Base64 to Image
    img_data = base64.b64decode(encoded_content)
    img = PILImage.open(BytesIO(img_data))
    
    # Save the Image as JPEG
    jpeg_path = f"/tmp/your-image-name-{datetime.now().strftime('%Y%m%d%H%M%S')}.jpeg"
    img.save(jpeg_path, 'JPEG')

    # Generate captions using the saved JPEG
    with open(jpeg_path, 'rb') as f:
        img_bytes = f.read()

    source_image = Image(img_bytes)

    captions = model.get_captions(
        image=source_image,
        number_of_results=2,
        language="en"
    )

    """ 
    captions = model.ask_question(
        image=source_image,
        question="Are these bananas unripe, barely ripe, ripe or overripe?",
        number_of_results=2,
    )
    """
    
    print(captions)

    # Generate text embeddings
    texts = ["Unripe Bananas", "Barely Ripe Bananas", "Ripe Bananas", "Overripe Bananas", "This is not a fruit"]
    text_embeddings = {}
    for text in texts:
        instance = struct_pb2.Struct()
        instance.fields['text'].string_value = text
        response = client.predict(endpoint=endpoint, instances=[instance])
        text_embeddings[text] = response.predictions[0]['textEmbedding']
    
    print('Generated text embeddings')
    
    # Calculate similarity scores
    similarity_scores = {}
    for text, embedding in text_embeddings.items():
        score = cosine_similarity(np.array([image_embedding]), np.array([embedding]))[0][0]
        similarity_scores[text] = score
    
    print('Calculated similarity scores')
    
    # Find the highest-ranked label
    highest_ranked_label = max(similarity_scores, key=similarity_scores.get)
    
    print(f'Highest ranked label: {highest_ranked_label}')
    
    # Map labels to descriptive statements
    label_to_statement = {
        "Unripe Bananas": "These bananas are Underripe.",
        "Barely Ripe Bananas": "These bananas are Barely Ripe.",
        "Ripe Bananas": "These bananas are Perfectly Ripe.",
        "Overripe Bananas": "These bananas are Overripe.",
        "This is not a fruit": "There are no bananas in this image."
    }

    # Return the corresponding descriptive statement
    result = label_to_statement.get(highest_ranked_label, "Unknown ripeness")
    print(f'Returning description: {result}')

    # First caption
    first_caption = captions[0] if captions else "No caption available"
    
    # Combine the description, public URL and caption into a JSON dictionary
    return json.dumps({"description": result, "public_url": public_url, "caption": first_caption})
