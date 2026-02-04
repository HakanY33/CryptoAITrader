from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import google.generativeai as genai
import os

# --- AYARLAR ---
# API Key'ini buraya yapıştır
GOOGLE_API_KEY = "YourApiKey" 

# Gemini'yi Ayarla
genai.configure(api_key=GOOGLE_API_KEY)
model = genai.GenerativeModel('models/gemini-flash-latest')

app = FastAPI()

# --- GÜNCELLENMİŞ VERİ PAKETİ ---
# Android'deki "Money Printer" stratejisinden gelen tüm verileri buraya ekledik
class MarketData(BaseModel):
    symbol: str       # Ör: BTC-USDT
    price: str        # Ör: 97500.50
    trend: str        # Ör: YÜKSELİŞ EĞİLİMİ (EMA 200 Filtresi)
    rsi_status: str   # Ör: RSI: 45 (Nötr)
    ob_status: str    # Ör: Bullish OB Var
    fvg_status: str   # Ör: FVG Yok
    setup_entry: str  # Ör: 97200 (Trend Desteği)
    setup_tp: str     # Ör: 98500
    setup_sl: str     # Ör: 96800

@app.get("/")
def home():
    return {"message": "Crypto AI Trader V2.0 Backend Çalışıyor!"}

@app.post("/ask-ai")
def ask_gemini(data: MarketData):
    try:
        # --- YENİ HYBRID MONEY PRINTER PROMPTU ---
        prompt = f"""
        ROLÜN:
        Sen "Crypto AI Trader" adlı gelişmiş bir algoritmik trade sisteminin Baş Analistisin. 
        Sistemin "Universal Money Printer (Hybrid)" adlı motoru kullanıyor.
        
        ALGORİTMA MANTIĞI (Bunu bilerek yorumla):
        1. Trend Filtresi: Fiyat EMA 200 üzerindeyse sadece LONG, altındaysa sadece SHORT bakar.
        2. Sinyal: Smoothed RSI momentumu ve SMC (Order Block/FVG) yapılarını teyit eder.
        3. Risk Yönetimi: ATR bazlı dinamik Stop Loss kullanır. Asla stopsuz işlem açmaz.

        ANALİZ EDİLECEK VERİLER:
        - Parite: {data.symbol}
        - Anlık Fiyat: {data.price}
        - Algoritma Trend Tespiti: {data.trend}
        - SMC Yapısı: {data.ob_status} | {data.fvg_status}
        - RSI Durumu: {data.rsi_status}
        
        ALGORİTMANIN ÖNERDİĞİ SETUP:
        - Giriş: {data.setup_entry}
        - Hedef (TP): {data.setup_tp}
        - Stop (SL): {data.setup_sl}

        GÖREVİN:
        Algoritmanın teknik çıktısını bir "İnsan Uzman" gözüyle süzgeçten geçir ve kullanıcıya güven ver.
        
        1. **Setup Kalitesi:** Algoritmanın verdiği Entry/TP/SL mantıklı mı? (Örn: Trend yönünde mi?)
        2. **SMC Teyidi:** Order Block veya FVG bu işlemi destekliyor mu?
        3. **Yatırımcı Psikolojisi:** Kullanıcıya disiplinli olmasını, ATR stopuna sadık kalmasını hatırlat.
        
        KURALLAR:
        - Asla "Yatırım tavsiyesidir" deme.
        - Tonun: Profesyonel, Analitik, Güven Veren ve Kısa.
        - Emoji kullanımı: Minimum ve yerinde (Örn: 🚀, 🛑).
        - Cevabın maksimum 3-4 cümle olsun. Uzatma.

        """

        # Gemini'ye sor
        response = model.generate_content(prompt)
        
        return {"ai_response": response.text}

    except Exception as e:
        return {"ai_response": f"Hata oluştu: {str(e)}"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)