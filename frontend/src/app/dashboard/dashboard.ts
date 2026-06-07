import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../auth';
import { ApiService } from '../api';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  role: string | null = null;
  jobs: any[] = [];
  activeTab: string = 'overview';
  
  // Recruiter specific
  newJob = { title: '', description: '', requiredSkills: '' };
  jobApplications: { [jobId: number]: any[] } = {};
  recruiterStats = {
    totalJobs: 0,
    totalApplicants: 0,
    pendingCount: 0,
    averageScore: 0
  };
  
  // Candidate specific
  myApplications: any[] = [];
  cvUploaded = false;
  uploading = false;
  myCv: any = null;
  applyingJobId: number | null = null;
  candidateStats = {
    appliedCount: 0,
    acceptedCount: 0,
    pendingCount: 0,
    averageScore: 0
  };

  constructor(private auth: AuthService, private api: ApiService, private router: Router) {}

  ngOnInit() {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/login']);
      return;
    }
    
    this.role = this.auth.getUserRole();
    
    if (this.role === 'RECRUITER') {
      this.loadRecruiterData();
    } else if (this.role === 'CANDIDATE') {
      this.loadCandidateData();
    }
  }

  setTab(tab: string) {
    this.activeTab = tab;
  }

  private cleanDate(dStr: any): any {
    if (typeof dStr === 'string') {
      return dStr.replace(/\[[^\]]+\]$/, '');
    }
    return dStr;
  }

  loadRecruiterData() {
    this.api.getJobs().subscribe((res: any) => {
      this.jobs = res;
      this.recruiterStats.totalJobs = res.length;
      
      let allApps: any[] = [];
      let appsFetched = 0;

      if (this.jobs.length === 0) {
        this.recruiterStats.totalApplicants = 0;
        this.recruiterStats.pendingCount = 0;
        this.recruiterStats.averageScore = 0;
        return;
      }

      this.jobs.forEach(job => {
        this.api.getApplicationsForJob(job.id).subscribe((apps: any) => {
          const cleanedApps = (apps || []).map((app: any) => {
            if (app.applicationDate) {
              app.applicationDate = this.cleanDate(app.applicationDate);
            }
            return app;
          });
          this.jobApplications[job.id] = cleanedApps;
          allApps.push(...cleanedApps);
          
          appsFetched++;
          if (appsFetched === this.jobs.length) {
            this.recruiterStats.totalApplicants = allApps.length;
            this.recruiterStats.pendingCount = allApps.filter(a => a.status === 'PENDING').length;
            
            const scores = allApps.map(a => a.score).filter(s => s != null);
            this.recruiterStats.averageScore = scores.length > 0 
              ? Math.round(scores.reduce((a, b) => a + b, 0) / scores.length) 
              : 0;
          }
        });
      });
    });
  }

  loadCandidateData() {
    this.api.getJobs().subscribe((res: any) => this.jobs = res);
    this.api.getMyApplications().subscribe((res: any) => {
      this.myApplications = (res || []).map((app: any) => {
        if (app.applicationDate) {
          app.applicationDate = this.cleanDate(app.applicationDate);
        }
        return app;
      });
      
      this.candidateStats.appliedCount = this.myApplications.length;
      this.candidateStats.acceptedCount = this.myApplications.filter(a => a.status === 'ACCEPTED').length;
      this.candidateStats.pendingCount = this.myApplications.filter(a => a.status === 'PENDING').length;
      
      const scores = this.myApplications.map(a => a.score).filter(s => s != null);
      this.candidateStats.averageScore = scores.length > 0 
        ? Math.round(scores.reduce((a, b) => a + b, 0) / scores.length) 
        : 0;
    });
    this.api.getMyCv().subscribe(
      (res: any) => {
        if (res && res.uploadDate) {
          res.uploadDate = this.cleanDate(res.uploadDate);
        }
        this.myCv = res;
        this.cvUploaded = true;
      },
      (err) => {
        this.myCv = null;
        this.cvUploaded = false;
      }
    );
  }

  // Recruiter Actions
  createJob() {
    const payload = {
      title: this.newJob.title,
      description: this.newJob.description,
      requiredSkills: this.newJob.requiredSkills.split(',').map(s => s.trim())
    };
    this.api.createJob(payload).subscribe(() => {
      this.newJob = { title: '', description: '', requiredSkills: '' };
      this.loadRecruiterData();
    });
  }

  deleteJob(id: number) {
    this.api.deleteJob(id).subscribe(() => this.loadRecruiterData());
  }

  updateStatus(appId: number, status: string) {
    this.api.updateApplicationStatus(appId, status).subscribe(() => {
      this.loadRecruiterData();
    });
  }

  reanalyze(appId: number) {
    this.api.reanalyzeApplication(appId).subscribe(() => {
      this.loadRecruiterData();
      alert("AI analysis updated successfully!");
    }, (err) => {
      console.error(err);
      alert("Error re-running AI analysis: " + (err.error?.error || err.message));
    });
  }

  downloadCv(candidateId: number) {
    this.api.downloadCv(candidateId).subscribe(blob => {
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'candidate_cv.pdf'; // Or we could parse from Content-Disposition if we had access to headers
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    });
  }

  downloadMyCv() {
    const candidateId = this.auth.getUserId();
    if (candidateId) {
      this.downloadCv(candidateId);
    } else {
      alert("Impossible de récupérer votre ID utilisateur.");
    }
  }

  // Candidate Actions
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.uploading = true;
      const reader = new FileReader();
      reader.onload = (e: any) => {
        const base64 = e.target.result.split(',')[1];
        this.api.uploadCv(file.name, base64).subscribe((res: any) => {
          if (res && res.uploadDate) {
            res.uploadDate = this.cleanDate(res.uploadDate);
          }
          this.cvUploaded = true;
          this.myCv = res;
          this.uploading = false;
          alert("CV Uploaded Successfully! You can now apply to jobs.");
        }, (err) => {
          this.uploading = false;
          console.error("Upload error details:", err);
          alert("Error uploading CV: " + (err.error?.error || err.message || JSON.stringify(err)));
        });
      };
      reader.readAsDataURL(file);
    }
  }

  apply(jobId: number) {
    this.applyingJobId = jobId;
    this.api.applyForJob(jobId).subscribe(() => {
      this.applyingJobId = null;
      this.loadCandidateData();
      alert("Application submitted successfully!");
    }, (err) => {
      this.applyingJobId = null;
      if (err.status === 500) {
        alert("Server Error. Please ensure you have uploaded a CV first!");
      } else {
        alert(err.error?.error || "Error applying for job");
      }
    });
  }

  getParsedImprovements(): { title: string, text: string }[] {
    if (!this.myCv || !this.myCv.aiImprovements) return [];
    
    const raw = this.myCv.aiImprovements;
    const paragraphs = raw.split(/\n+/);
    
    return paragraphs.map((p: string) => {
      const match = p.match(/^\d+\.\s*(.*?)\s*:\s*(.*)/);
      if (match) {
        return { title: match[1], text: match[2] };
      }
      return { title: 'Conseil', text: p.replace(/^\d+\.\s*/, '') };
    }).filter((item: any) => item.text.trim().length > 0);
  }

  hasApplied(jobId: number) {
    return this.myApplications.some(app => app.jobOffer?.id === jobId);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
