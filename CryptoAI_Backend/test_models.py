import google.generativeai as genai

# API Key'ini buraya yapıştır
GOOGLE_API_KEY = "AIzaSyAbneFyUOLT1Y769GU0Z2vSizysSGSTIcU"
genai.configure(api_key=GOOGLE_API_KEY)

print("--- ERIŞILEBILIR MODELLER ---")
try:
    for m in genai.list_models():
        # Sadece metin üretebilen modelleri göster
        if 'generateContent' in m.supported_generation_methods:
            print(f"Model Adı: {m.name}")
except Exception as e:
    print(f"Hata oluştu: {e}")