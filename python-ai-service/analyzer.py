import pdfplumber
import os
import json
from google import genai

# Configure Gemini
api_key = os.environ.get("GEMINI_API_KEY", "AIzaSyCKFjhEudg6Pj67whhDhz-l1Z_4Psr_kqY")
client = genai.Client(api_key=api_key)

def extract_text_from_pdf(pdf_path: str) -> str:
    text = ""
    try:
        with pdfplumber.open(pdf_path) as pdf:
            for page in pdf.pages:
                page_text = page.extract_text()
                if page_text:
                    text += page_text + "\n"
    except Exception as e:
        print(f"Error reading PDF: {e}")
    return text

def analyze_cv_general(text: str) -> str:
    prompt = f"""
    You are an expert HR consultant. Review the following CV text and provide constructive improvements for the candidate to make their CV more appealing to recruiters.
    Keep the response concise (around 2-3 paragraphs) and highly professional. Focus on actionable advice.
    
    CV Text:
    {text[:5000]}
    """
    try:
        response = client.models.generate_content(
            model='gemini-2.5-flash',
            contents=prompt,
        )
        return response.text
    except Exception as e:
        print(f"Error calling Gemini: {e}")
        return "The AI service is currently unavailable for general improvements."

def analyze_job_fit(text: str, required_skills: list[str]) -> dict:
    skills_str = ", ".join(required_skills)
    prompt = f"""
    You are an expert ATS (Applicant Tracking System) and HR recruiter. 
    Analyze the following CV against the required skills for a job: {skills_str}.
    
    You must return a valid JSON object strictly adhering to this format, without markdown blocks:
    {{
        "score": <an integer between 0 and 100 representing the overall match percentage>,
        "strongest_points": "<a concise paragraph detailing why this candidate is a good fit based on the required skills>"
    }}
    
    CV Text:
    {text[:5000]}
    """
    try:
        response = client.models.generate_content(
            model='gemini-2.5-flash',
            contents=prompt,
        )
        
        # Simple JSON extraction in case it adds markdown backticks
        res_text = response.text.strip()
        if res_text.startswith("```json"):
            res_text = res_text[7:-3].strip()
        elif res_text.startswith("```"):
            res_text = res_text[3:-3].strip()
            
        return json.loads(res_text)
    except Exception as e:
        print("Failed to parse JSON from Gemini:", e)
        return {"score": 50, "strongest_points": "Could not parse AI response or AI service unavailable."}
