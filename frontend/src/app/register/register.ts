import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css'
})
export class Register {
  user = { name: '', email: '', password: '', role: 'CANDIDATE' };
  error = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit() {
    if (!this.user.name || !this.user.email || !this.user.password) {
      this.error = 'Please fill in all fields';
      return;
    }

    // Split name into first/last for the backend DTO
    const nameParts = this.user.name.trim().split(' ');
    const firstName = nameParts[0];
    const lastName = nameParts.length > 1 ? nameParts.slice(1).join(' ') : 'User';

    const payload = {
      firstName: firstName,
      lastName: lastName,
      email: this.user.email,
      password: this.user.password,
      role: this.user.role,
      companyName: 'N/A',
      phone: '0000000000',
      address: 'N/A'
    };

    this.loading = true;
    this.error = '';
    
    this.authService.register(payload).subscribe({
      next: (res: any) => {
        // Auto-login after successful registration
        this.authService.login({ email: this.user.email, password: this.user.password }).subscribe({
          next: (loginRes: any) => {
            this.authService.setToken(loginRes.token);
            this.router.navigate(['/dashboard']);
          },
          error: () => {
            // Fallback to login page if auto-login fails
            this.router.navigate(['/login']);
          }
        });
      },
      error: (err) => {
        this.error = err.error?.error || err.error?.message || 'Registration failed. Email might already exist.';
        this.loading = false;
      }
    });
  }
}
