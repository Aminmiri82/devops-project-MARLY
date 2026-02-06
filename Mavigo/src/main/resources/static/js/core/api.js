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
    if (body !== undefined) {
      opts.headers["Content-Type"] = "application/json";
      opts.body = JSON.stringify(body);
    }
    const resp = await fetch(url, opts);
    if (!resp.ok) {
      if (resp.status === 401 || resp.status === 403 || resp.status === 409) {
        const error = new Error("Google Tasks not authorized");
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
