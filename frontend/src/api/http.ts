import axios from "axios";
import { ElMessage } from "element-plus";
const http = axios.create({ baseURL: "/api", timeout: 12000 });
http.interceptors.request.use((config) => {
  const token = localStorage.getItem("petclinic-token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
http.interceptors.response.use(
  (r) => r.data,
  (e) => {
    const status = e.response?.status;
    const message = e.response?.data?.message || e.message;
    if (status === 501)
      ElMessage.warning("该模块已定义接口，等待你完成后端实现");
    else if (status === 401) {
      localStorage.removeItem("petclinic-token");
      localStorage.removeItem("petclinic-user");
      if (location.pathname != "/login") location.href = "/login";
    } else ElMessage.error(message || "请求失败");
    return Promise.reject(e);
  },
);
export default http;
