export const api = {
  async get(url) {
    return this.request(url, "GET");
  },
  async post(url, body) {
    return this.request(url, "POST", body);
  },
  async put(url, body) {
    return this.request(url, "PUT", body);
  },
  async patch(url, body) {
    return this.request(url, "PATCH", body);
  },
  async delete(url) {
    return this.request(url, "DELETE");
  },
  async request(url, method, body) {
    const opts = { method, headers: {} };
    const token = typeof localStorage !== "undefined" ? localStorage.getItem("mavigo_token") : null;
    if (token) {
      opts.headers["Authorization"] = "Bearer " + token;
    }
    if (body !== undefined) {
      opts.headers["Content-Type"] = "application/json";
      opts.body = JSON.stringify(body);
    }
    const resp = await fetch(url, opts);
    if (!resp.ok) {
      if (resp.status === 401 || resp.status === 403) {
        const error = new Error("Session expired or unauthorized");
        error.authError = true;
        throw error;
      }
      const text = await resp.text();
      throw new Error(text || `Request failed: ${resp.status}`);
    }
    const contentType = resp.headers.get("content-type");
    if (contentType?.includes("application/json")) {
      return resp.json();
    }
    return resp.text();
  },
  async getEcoDashboard(userId) {
    return this.get(`/api/eco/dashboard?userId=${userId}`);
  },
};
