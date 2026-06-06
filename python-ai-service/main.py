from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import os
from analyzer import extract_text_from_pdf, analyze_cv_general, analyze_job_fit

app = FastAPI(title="CV Analysis AI Microservice")

class GeneralAnalysisRequest(BaseModel):
    file_path: str

class GeneralAnalysisResponse(BaseModel):
    improvements: str

class JobFitRequest(BaseModel):
    file_path: str
    required_skills: list[str]

class JobFitResponse(BaseModel):
    score: int
    strongest_points: str

@app.post("/analyze_cv_general", response_model=GeneralAnalysisResponse)
def analyze_general(request: GeneralAnalysisRequest):
    if not os.path.exists(request.file_path):
        raise HTTPException(status_code=404, detail="CV file not found on disk")
    text = extract_text_from_pdf(request.file_path)
    if not text.strip():
        raise HTTPException(status_code=400, detail="Could not extract text from CV")
        
    improvements = analyze_cv_general(text)
    return GeneralAnalysisResponse(improvements=improvements)

@app.post("/analyze", response_model=JobFitResponse)
def analyze_fit(request: JobFitRequest):
    print(f"DEBUG: Received file_path: '{request.file_path}'")
    if not os.path.exists(request.file_path):
        print(f"DEBUG: File not found: '{request.file_path}'")
        raise HTTPException(status_code=404, detail="CV file not found on disk")
    text = extract_text_from_pdf(request.file_path)
    if not text.strip():
        raise HTTPException(status_code=400, detail="Could not extract text from CV")
        
    result = analyze_job_fit(text, request.required_skills)
    return JobFitResponse(score=result["score"], strongest_points=result["strongest_points"])

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
