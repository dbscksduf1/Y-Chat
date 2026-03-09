import axios from "axios";

const instance = axios.create({
  baseURL: "https://y-chat-my45.onrender.com",
});

instance.interceptors.request.use((config) => {
  const token = sessionStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default instance;