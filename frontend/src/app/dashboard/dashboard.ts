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
  
  // Recruiter specific
  newJob = { title: '', description: '', requiredSkills: '' };
  jobApplications: { [jobId: number]: any[] } = {};
  
  // Candidate specific
  myApplications: any[] = [];
  cvUploaded = false;
  uploading = false;
  myCv: any = null;
  applyingJobId: number | null = null;

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

  loadRecruiterData() {
    this.api.getJobs().subscribe((res: any) => {
      this.jobs = res;
      // Fetch applications for each job
      this.jobs.forEach(job => {
        this.api.getApplicationsForJob(job.id).subscribe((apps: any) => {
          this.jobApplications[job.id] = apps;
        });
      });
    });
  }

  loadCandidateData() {
    this.api.getJobs().subscribe((res: any) => this.jobs = res);
    this.api.getMyApplications().subscribe((res: any) => this.myApplications = res);
    this.api.getMyCv().subscribe(
      (res: any) => {
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

  // Candidate Actions
  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.uploading = true;
      const reader = new FileReader();
      reader.onload = (e: any) => {
        const base64 = e.target.result.split(',')[1];
        this.api.uploadCv(file.name, base64).subscribe((res: any) => {
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

  hasApplied(jobId: number) {
    return this.myApplications.some(app => app.jobOffer?.id === jobId);
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
