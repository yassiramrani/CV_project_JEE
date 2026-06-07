import pdfplumber
import os
import json
from google import genai

# Configure Gemini
env_key = None
dotenv_path = os.path.join(os.path.dirname(__file__), ".env")
if os.path.exists(dotenv_path):
    try:
        with open(dotenv_path, "r", encoding="utf-8") as f:
            for line in f:
                if line.strip().startswith("GEMINI_API_KEY="):
                    env_key = line.strip().split("GEMINI_API_KEY=")[1].strip().strip('"').strip("'")
                    break
    except Exception as e:
        print("Error reading .env file:", e)

api_key = env_key or os.environ.get("GEMINI_API_KEY")
if not api_key or api_key == "VOTRE_CLE_API_GEMINI_ICI":
    print("WARNING: Gemini API Key is not set! Please edit python-ai-service/.env to set your key.")

client = genai.Client(api_key=api_key or "DUMMY_KEY")

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
        print(f"Error calling Gemini: {e}. Using local fallback analysis.")
        # Local mock recommendations based on the CV content
        skills_detected = []
        for s in ["java", "python", "angular", "react", "sql", "git", "docker", "spring", "javascript", "typescript", "html", "css", "c#", "php"]:
            if s in text.lower():
                skills_detected.append(s.capitalize())
        
        skills_str = ", ".join(skills_detected) if skills_detected else "développement informatique"
        
        return f"1. Mise en valeur des compétences : Nous avons détecté des compétences comme {skills_str}. Structurez clairement votre CV en ajoutant des sections dédiées aux projets réalisés pour capter l'intérêt des recruteurs.\n\n2. Ajout de réalisations quantifiables : Détaillez vos expériences passées en incluant des chiffres et métriques précis (ex: temps de chargement réduits de 30%, équipe de 4 développeurs).\n\n3. Clarté de la présentation : Optimisez la mise en page de votre document (polices standardisées, aération constante) pour maximiser la lisibilité automatique par les systèmes ATS."

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
        print("Failed to parse JSON from Gemini or API Error:", e, ". Using local fallback calculation.")
        # Calculate a realistic score based on matching skills
        matching_skills = []
        missing_skills = []
        for skill in required_skills:
            if skill.lower().strip() in text.lower():
                matching_skills.append(skill.strip())
            else:
                missing_skills.append(skill.strip())
        
        # Compute match percentage
        total = len(required_skills)
        if total > 0:
            score = int((len(matching_skills) / total) * 100)
            # Add some baseline score if they have general profile matches
            if score < 45 and len(matching_skills) > 0:
                score = 55
            elif score == 0:
                score = 30
        else:
            score = 75
            
        if matching_skills:
            strong_points = f"Le candidat montre une bonne correspondance avec les compétences clés recherchées, notamment : {', '.join(matching_skills)}. "
            if missing_skills:
                strong_points += f"Il est recommandé de se perfectionner sur : {', '.join(missing_skills)}."
            else:
                strong_points += "Le profil correspond parfaitement aux exigences techniques du poste."
        else:
            strong_points = f"Le CV ne mentionne pas explicitement les compétences techniques requises ({', '.join(required_skills)}). Une formation complémentaire ou des projets personnels sur ces technologies sont conseillés."
            
        return {
            "score": score,
            "strongest_points": strong_points
        }
