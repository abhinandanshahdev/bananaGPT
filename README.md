# bananaGPT
BananaGPT is a computer vision application that checks for ripeness of bananas using google's vertex ai multi modal embeddings and cosine similarity. It can automatically schedule checks every few minutes or hours, and then send a whatsapp message when it finds something meaningful. 

This is a very basic implementation meant for educational purposes only. I have not tested the app for memory leaks or battery drains and I would not be responsible for any issues. 

The solution has 2 parts: 

1) Android App

I have uploaded key files directly in the main branch as I needed to redact some stuff, but key step is to capture an image and send it as a payload to google cloud function using a simple API call

2) Google cloud function

This is also redacted but this needs requirements.txt, this needs project to be setup with appropriate services enabled on your google cloud account. Vertex AI, cloud Run Service etc. among them.

If you have any questions, feel free to reach out via LinkedIn.
