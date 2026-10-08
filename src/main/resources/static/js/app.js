// RecoMind Central JS App Utility

const App = {
    csrfToken: localStorage.getItem('recomind_csrf') || '',
    user: null,

    // Initialize theme on load
    initTheme() {
        const saved = localStorage.getItem('recomind_theme') || 'dark';
        document.documentElement.setAttribute('data-theme', saved);
        this.updateThemeButton(saved);
    },

    toggleTheme() {
        const current = document.documentElement.getAttribute('data-theme') || 'dark';
        const next = current === 'dark' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', next);
        localStorage.setItem('recomind_theme', next);
        this.updateThemeButton(next);
    },

    updateThemeButton(theme) {
        const btn = document.getElementById('theme-toggle');
        if (btn) {
            btn.innerHTML = theme === 'dark' ? '☀️' : '🌙';
            btn.title = theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode';
        }
    },

    // Toast Notification System
    showToast(message, type = 'info') {
        let container = document.getElementById('toast-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'toast-container';
            document.body.appendChild(container);
        }

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ';
        toast.innerHTML = `<span style="font-weight: bold;">${icon}</span> <span>${this.escapeHtml(message)}</span>`;
        container.appendChild(toast);

        setTimeout(() => {
            toast.style.opacity = '0';
            toast.style.transform = 'translateY(10px)';
            toast.style.transition = 'all 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3500);
    },

    // Safe API Fetch Wrapper
    async fetchJson(url, options = {}) {
        const defaultHeaders = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        };

        if (this.csrfToken && (options.method === 'POST' || options.method === 'PUT' || options.method === 'DELETE')) {
            defaultHeaders['X-CSRF-Token'] = this.csrfToken;
        }

        const config = {
            ...options,
            headers: {
                ...defaultHeaders,
                ...(options.headers || {})
            }
        };

        try {
            const resp = await fetch(url, config);

            // If session expired or unauthorized, redirect to login unless on public auth pages
            if (resp.status === 401) {
                if (!window.location.pathname.endsWith('login.html') && !window.location.pathname.endsWith('register.html')) {
                    window.location.href = '/login.html';
                }
                const errJson = await resp.json().catch(() => ({ message: 'Unauthorized' }));
                throw new Error(errJson.message || 'Please log in');
            }

            const data = await resp.json().catch(() => ({ success: false, message: 'Invalid server response' }));

            if (!resp.ok || !data.success) {
                throw new Error(data.message || `Request failed with status ${resp.status}`);
            }

            // If response returned a new csrfToken, update it
            if (data.data && data.data.csrfToken) {
                this.csrfToken = data.data.csrfToken;
                localStorage.setItem('recomind_csrf', this.csrfToken);
            }

            return data;
        } catch (err) {
            console.error(`API Error on ${url}:`, err);
            throw err;
        }
    },

    // Check Auth State
    async checkAuth() {
        try {
            const res = await this.fetchJson('/api/auth/me');
            this.user = res.data;
            if (res.data.csrfToken) {
                this.csrfToken = res.data.csrfToken;
                localStorage.setItem('recomind_csrf', this.csrfToken);
            }
            return this.user;
        } catch (err) {
            this.user = null;
            return null;
        }
    },

    // Logout
    async logout() {
        try {
            await this.fetchJson('/api/auth/logout', { method: 'POST' });
        } catch (ignored) {}
        localStorage.removeItem('recomind_csrf');
        window.location.href = '/login.html';
    },

    // Render Navigation Sidebar
    renderSidebar(activePage = 'dashboard') {
        const sidebar = document.querySelector('.sidebar');
        if (!sidebar) return;

        const isAdmin = this.user && this.user.role === 'ADMIN';

        sidebar.innerHTML = `
            <div style="padding: 1.5rem 1.25rem 1rem;">
                <a href="/dashboard.html" class="brand">
                    <div class="brand-icon">R</div>
                    <span class="brand-name">RecoMind</span>
                </a>
            </div>
            <nav class="sidebar-nav">
                <a href="/dashboard.html" class="nav-link ${activePage === 'dashboard' ? 'active' : ''}">
                    <span class="nav-icon">⚡</span> Dashboard
                </a>
                <a href="/discover.html" class="nav-link ${activePage === 'discover' ? 'active' : ''}">
                    <span class="nav-icon">🔍</span> Discover
                </a>
                <a href="/interactions.html" class="nav-link ${activePage === 'interactions' ? 'active' : ''}">
                    <span class="nav-icon">❤️</span> My Interactions
                </a>
                <a href="/preferences.html" class="nav-link ${activePage === 'preferences' ? 'active' : ''}">
                    <span class="nav-icon">⚙️</span> Preferences
                </a>
                <a href="/profile.html" class="nav-link ${activePage === 'profile' ? 'active' : ''}">
                    <span class="nav-icon">👤</span> Profile
                </a>
                ${isAdmin ? `
                <div style="margin: 0.75rem 0 0.25rem 1rem; font-size: 0.75rem; font-weight: 700; color: var(--text-muted); text-transform: uppercase;">Admin</div>
                <a href="/admin.html" class="nav-link ${activePage === 'admin' ? 'active' : ''}">
                    <span class="nav-icon">🛡️</span> Admin Console
                </a>
                ` : ''}
            </nav>
            <div class="sidebar-footer">
                <div style="display: flex; align-items: center; gap: 0.75rem; overflow: hidden;">
                    <div style="width: 32px; height: 32px; border-radius: var(--radius-full); background: var(--bg-tertiary); display: flex; align-items: center; justify-content: center; font-size: 0.85rem; font-weight: bold; flex-shrink: 0;">
                        ${this.user ? this.user.email.charAt(0).toUpperCase() : 'U'}
                    </div>
                    <div style="overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 0.85rem;">
                        ${this.user ? this.user.email : ''}
                    </div>
                </div>
                <div style="display: flex; gap: 0.35rem;">
                    <button id="theme-toggle" class="theme-toggle-btn" onclick="App.toggleTheme()">🌙</button>
                    <button class="theme-toggle-btn" title="Logout" onclick="App.logout()">🚪</button>
                </div>
            </div>
        `;
        this.updateThemeButton(document.documentElement.getAttribute('data-theme') || 'dark');
    },

    escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }
};

document.addEventListener('DOMContentLoaded', () => {
    App.initTheme();
});
