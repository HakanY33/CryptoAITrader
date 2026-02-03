from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import google.generativeai as genai
import os

# --- AYARLAR ---
# Buraya Google AI Studio'dan aldığın key'i yapıştır
GOOGLE_API_KEY = "YourApiKey" 

# Gemini'yi Ayarla
genai.configure(api_key=GOOGLE_API_KEY)
model = genai.GenerativeModel('models/gemini-flash-latest')

app = FastAPI()

# Android'den gelecek veri paketi
class MarketData(BaseModel):
    symbol: str       # Ör: BTC-USDT
    price: str        # Ör: 97500.50
    ema21: str        # Ör: 97400
    ema50: str        # Ör: 97600
    trend: str        # Ör: YÜKSELİŞ
    ob_status: str    # Ör: Bullish OB Var
    fvg_status: str   # Ör: FVG Yok

@app.get("/")
def home():
    return {"message": "Crypto AI Backend Çalışıyor!"}

@app.post("/ask-ai")
def ask_gemini(data: MarketData):
    try:
        # --- SİHİRLİ PROMPT (SENİN STRATEJİN) ---
        prompt = f"""
        Rolün: Sen profesyonel, soğukkanlı ve matematiksel düşünen bir Kripto Vadeli İşlemler (Futures) Uzmanısın.
        
        KULLANICI PROFİLİ:
        - Stil: Agresif Scalper / Day Trader.
        - Strateji: Yüksek Kaldıraç (100x), Düşük Marj. Stop Loss yerine DCA (Kademeli Ekleme) ve Likidite Yönetimi.
        - Terimler: Asla "Al" veya "Sat" deme. Daima "LONG İşlem", "SHORT İşlem", "Pozisyon Kapat", "Ekleme Yap" terimlerini kullan.

        PİYASA VERİLERİ:
        - Parite: {data.symbol}
        - Fiyat: {data.price}
        - EMA 21/50: {data.ema21} / {data.ema50}
        - Teknik Trend: {data.trend} (Bu trend 6 farklı indikatörün oylaması sonucu çıktı)
        - SMC (Smart Money): {data.ob_status} ve {data.fvg_status}

        GÖREVİN:
        Bu verilere dayanarak net bir işlem planı kur.
        1. Yön ne olmalı? (Long mu Short mu yoksa İşlem Yok mu?)
        2. Nereden girmeli? (Örn: FVG bölgesinden veya EMA testinden)
        3. Risk uyarısı yapma, sadece stratejiye odaklan.
        
        Cevabın kısa, Türkçe ve profesyonel trader ağzıyla olsun.
        """

        # Gemini'ye sor
        response = model.generate_content(prompt)
        
        # Cevabı Android'e gönder
        return {"ai_response": response.text}

    except Exception as e:
        return {"ai_response": f"Hata oluştu: {str(e)}"}

# Bu dosya direkt çalıştırılırsa sunucuyu başlat
if __name__ == "__main__":
    import uvicorn
    # host="0.0.0.0" demek, ağdaki diğer cihazlar (Android Emulator) erişebilir demek.
    uvicorn.run(app, host="0.0.0.0", port=8000)